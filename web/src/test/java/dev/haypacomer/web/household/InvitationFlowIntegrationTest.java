package dev.haypacomer.web.household;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.application.port.EmailSender;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class InvitationFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  static final List<EmailMessage> OUTBOX = new CopyOnWriteArrayList<>();

  @TestConfiguration
  static class CapturingMail {

    @Bean
    @Primary
    EmailSender capturingEmailSender() {
      return OUTBOX::add;
    }
  }

  @Autowired private MockMvc mvc;

  private String send(String path, String bearer, String body, int expected) throws Exception {
    var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      request.header("Authorization", bearer);
    }
    return mvc.perform(request)
        .andExpect(status().is(expected))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static String lastTokenFor(String email) {
    EmailMessage message =
        OUTBOX.reversed().stream()
            .filter(item -> item.to().value().equals(email))
            .findFirst()
            .orElseThrow();
    String link = message.link();
    return URLDecoder.decode(link.substring(link.indexOf("#token=") + 7), StandardCharsets.UTF_8);
  }

  private String signUp(String email, String password) throws Exception {
    send(
        "/api/v1/auth/register",
        null,
        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"displayName\":\"P\"}",
        201);
    return login(email, password);
  }

  private String login(String email, String password) throws Exception {
    String body =
        send(
            "/api/v1/auth/login",
            null,
            "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}",
            200);
    return "Bearer " + JsonPath.read(body, "$.accessToken");
  }

  @Test
  void verifyInviteAcceptAndResetPassword() throws Exception {
    String juan = signUp("juan@haypacomer.dev", "fresh-milk-842");
    String ana = signUp("ana@haypacomer.dev", "fresh-milk-842");

    String verified =
        send(
            "/api/v1/auth/verify-email",
            null,
            "{\"token\":\"" + lastTokenFor("juan@haypacomer.dev") + "\"}",
            200);
    assertTrue(JsonPath.<Boolean>read(verified, "$.emailVerified"));

    String household =
        send(
            "/api/v1/households",
            juan,
            "{\"name\":\"Apartment 402\",\"currency\":\"COP\",\"timezone\":\"America/Bogota\"}",
            201);
    String path = "/api/v1/households/" + JsonPath.read(household, "$.id");

    send(path + "/invitations", ana, "{\"email\":\"x@haypacomer.dev\",\"role\":\"GUEST\"}", 404);
    send(
        path + "/invitations", juan, "{\"email\":\"ana@haypacomer.dev\",\"role\":\"MEMBER\"}", 201);
    String leo =
        send(
            path + "/invitations",
            juan,
            "{\"email\":\"leo@haypacomer.dev\",\"role\":\"GUEST\"}",
            201);
    mvc.perform(get(path + "/invitations").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
    mvc.perform(
            delete(path + "/invitations/" + JsonPath.read(leo, "$.id"))
                .header("Authorization", juan))
        .andExpect(status().isNoContent());

    String token = lastTokenFor("ana@haypacomer.dev");
    send("/api/v1/invitations/accept", juan, "{\"token\":\"" + token + "\"}", 403);
    String joined = send("/api/v1/invitations/accept", ana, "{\"token\":\"" + token + "\"}", 200);
    assertTrue(JsonPath.<String>read(joined, "$.myRole").equals("MEMBER"));
    send("/api/v1/invitations/accept", ana, "{\"token\":\"" + token + "\"}", 400);

    send("/api/v1/auth/forgot-password", null, "{\"email\":\"ana@haypacomer.dev\"}", 202);
    send(
        "/api/v1/auth/reset-password",
        null,
        "{\"token\":\""
            + lastTokenFor("ana@haypacomer.dev")
            + "\",\"password\":\"new-password-123\"}",
        204);
    send(
        "/api/v1/auth/login",
        null,
        "{\"email\":\"ana@haypacomer.dev\",\"password\":\"fresh-milk-842\"}",
        401);
    login("ana@haypacomer.dev", "new-password-123");
  }
}
