package dev.haypacomer.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SecurityIntegrationTest {

  private static final String PASSWORD = "fresh-milk-842";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;
  @Autowired private JwtEncoder encoder;
  @Autowired private JwtProperties properties;
  @Autowired private FridgeRepository fridges;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping mappings;

  private String send(HttpMethod method, String path, String bearer, String body, int expected)
      throws Exception {
    var builder = request(method, path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      builder.header("Authorization", bearer);
    }
    return mvc.perform(builder)
        .andExpect(status().is(expected))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String signUp(String email) throws Exception {
    send(
        HttpMethod.POST,
        "/api/v1/auth/register",
        null,
        "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"displayName\":\"P\"}",
        201);
    String login =
        send(
            HttpMethod.POST,
            "/api/v1/auth/login",
            null,
            "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
            200);
    return "Bearer " + JsonPath.read(login, "$.accessToken");
  }

  private static String concrete(String pattern) {
    return pattern.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
  }

  @Test
  void everyEndpointOutsideTheAuthAllowlistRejectsAnonymousCallers() throws Exception {
    List<String> checked = new ArrayList<>();
    for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
      for (String pattern : info.getPatternValues()) {
        if (!pattern.startsWith("/api/")) {
          continue;
        }
        for (RequestMethod method : info.getMethodsCondition().getMethods()) {
          boolean publicEndpoint =
              method == RequestMethod.POST && pattern.startsWith("/api/v1/auth/");
          if (publicEndpoint) {
            continue;
          }
          MvcResult result =
              mvc.perform(
                      request(HttpMethod.valueOf(method.name()), concrete(pattern))
                          .contentType(MediaType.APPLICATION_JSON)
                          .content("{}"))
                  .andReturn();
          assertEquals(
              401, result.getResponse().getStatus(), method + " " + pattern + " must require auth");
          checked.add(method + " " + pattern);
        }
      }
    }
    assertTrue(checked.size() >= 15, "Expected to cover every protected endpoint: " + checked);
  }

  @Test
  void rejectsForgedExpiredUnsignedAndForeignTokens() throws Exception {
    signUp("juan@haypacomer.dev");
    UserId someone = UserId.newId();
    String expired =
        new JwtAccessTokenIssuer(encoder, properties)
            .issue(someone, Instant.now().minus(Duration.ofHours(2)))
            .value();
    JwtProperties foreign =
        new JwtProperties(
            "another-secret-with-at-least-32-bytes", properties.issuer(), Duration.ofMinutes(15));
    String forged =
        new JwtAccessTokenIssuer(new SecurityConfiguration().jwtEncoder(foreign), foreign)
            .issue(someone, Instant.now())
            .value();
    Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
    String unsigned =
        base64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
            + "."
            + base64.encodeToString(
                ("{\"sub\":\"" + someone.value() + "\",\"iss\":\"" + properties.issuer() + "\"}")
                    .getBytes(StandardCharsets.UTF_8))
            + ".";

    for (String token : List.of(expired, forged, unsigned, "garbage")) {
      mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void householdsAreIsolatedAndRolesAreEnforced() throws Exception {
    String owner = signUp("owner@haypacomer.dev");
    String outsider = signUp("outsider@haypacomer.dev");
    String household =
        send(
            HttpMethod.POST,
            "/api/v1/households",
            owner,
            "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}",
            201);
    String path = "/api/v1/households/" + JsonPath.read(household, "$.id");

    send(HttpMethod.GET, path, outsider, "", 404);
    send(HttpMethod.PATCH, path, outsider, "{\"name\":\"Mine\"}", 404);
    send(HttpMethod.GET, path + "/devices", outsider, "", 404);
    send(HttpMethod.GET, path + "/invitations", outsider, "", 404);
    send(
        HttpMethod.POST,
        path + "/invitations",
        outsider,
        "{\"email\":\"x@y.co\",\"role\":\"GUEST\"}",
        404);
    send(
        HttpMethod.PUT,
        path + "/owner",
        outsider,
        "{\"userId\":\"" + UUID.randomUUID() + "\"}",
        404);
    send(
        HttpMethod.POST,
        path + "/invitations",
        owner,
        "{\"email\":\"x@y.co\",\"role\":\"OWNER\"}",
        400);
  }

  @Test
  void deviceKeysAndUserTokensStayInTheirOwnLanes() throws Exception {
    String owner = signUp("lanes@haypacomer.dev");
    String household =
        send(
            HttpMethod.POST,
            "/api/v1/households",
            owner,
            "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}",
            201);
    UUID householdId = UUID.fromString(JsonPath.read(household, "$.id"));
    Fridge fridge = Fridge.named("Kitchen fridge");
    fridges.save(new HouseholdId(householdId), fridge);
    String registered =
        send(
            HttpMethod.POST,
            "/api/v1/households/" + householdId + "/devices",
            owner,
            "{\"fridgeId\":\""
                + fridge.id().value()
                + "\",\"name\":\"Door\",\"kind\":\"SIMULATOR\"}",
            201);
    String key = JsonPath.read(registered, "$.apiKey");

    mvc.perform(get("/api/v1/me").header("X-Device-Key", key)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + key))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/device/whoami").header("Authorization", owner))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/device/whoami").header("X-Device-Key", key))
        .andExpect(status().isOk());
  }

  @Test
  void responsesAreStatelessAndHardened() throws Exception {
    String bearer = signUp("headers@haypacomer.dev");

    MvcResult result =
        mvc.perform(get("/api/v1/me").header("Authorization", bearer))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andReturn();

    String cacheControl = result.getResponse().getHeader("Cache-Control");
    assertTrue(cacheControl != null && cacheControl.contains("no-store"));
    assertTrue(result.getResponse().getCookies().length == 0);
    assertFalse(result.getResponse().getContentAsString().contains("passwordHash"));
    assertFalse(result.getResponse().getContentAsString().contains(PASSWORD));
  }

  @Test
  void loginErrorsDoNotRevealWhichAccountsExist() throws Exception {
    signUp("real@haypacomer.dev");

    String wrongPassword =
        send(
            HttpMethod.POST,
            "/api/v1/auth/login",
            null,
            "{\"email\":\"real@haypacomer.dev\",\"password\":\"wrong-password\"}",
            401);
    String unknownAccount =
        send(
            HttpMethod.POST,
            "/api/v1/auth/login",
            null,
            "{\"email\":\"ghost@haypacomer.dev\",\"password\":\"wrong-password\"}",
            401);
    send(
        HttpMethod.POST,
        "/api/v1/auth/forgot-password",
        null,
        "{\"email\":\"ghost@haypacomer.dev\"}",
        202);
    send(
        HttpMethod.POST,
        "/api/v1/auth/forgot-password",
        null,
        "{\"email\":\"real@haypacomer.dev\"}",
        202);

    assertEquals(
        JsonPath.<String>read(wrongPassword, "$.detail"),
        JsonPath.<String>read(unknownAccount, "$.detail"));
    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }
}
