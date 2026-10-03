package dev.haypacomer.web.market;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class MarketFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;
  @Autowired private HouseholdRepository households;

  private ResultActions call(HttpMethod method, String path, String bearer, String body)
      throws Exception {
    var builder = request(method, path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      builder.header("Authorization", bearer);
    }
    return mvc.perform(builder);
  }

  private String signUp(String email) throws Exception {
    call(
            HttpMethod.POST,
            "/api/v1/auth/register",
            null,
            "{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\",\"displayName\":\"P\"}")
        .andExpect(status().isCreated());
    String login =
        call(
                HttpMethod.POST,
                "/api/v1/auth/login",
                null,
                "{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\"}")
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(login, "$.accessToken");
  }

  @Test
  void householdSharesOneDeduplicatedList() throws Exception {
    String juan = signUp("juan@haypacomer.dev");
    String ana = signUp("ana@haypacomer.dev");
    String anaId =
        JsonPath.read(
            call(HttpMethod.GET, "/api/v1/me", ana, "")
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    String created =
        call(
                HttpMethod.POST,
                "/api/v1/households",
                juan,
                "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}")
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID householdId = UUID.fromString(JsonPath.read(created, "$.id"));
    Household household = households.findById(new HouseholdId(householdId)).orElseThrow();
    household.join(new UserId(UUID.fromString(anaId)), Role.MEMBER, Instant.now());
    households.save(household);
    String list = "/api/v1/households/" + householdId + "/market-list";

    String rice =
        call(HttpMethod.POST, list + "/items", juan, "{\"food\":\"Rice\",\"grams\":500}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    call(HttpMethod.POST, list + "/items", ana, "{\"food\":\"rice\",\"grams\":250}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(JsonPath.<String>read(rice, "$.id")))
        .andExpect(jsonPath("$.grams").value(750.0));
    call(HttpMethod.POST, list + "/items", ana, "{\"food\":\"Chicken breast\",\"grams\":400}")
        .andExpect(status().isCreated());

    call(HttpMethod.GET, list, ana, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pending.length()").value(2))
        .andExpect(jsonPath("$.pending[0].category").value("POULTRY"))
        .andExpect(jsonPath("$.pending[1].items[0].food").value("Rice"));

    String riceItem = list + "/items/" + JsonPath.read(rice, "$.id");
    call(HttpMethod.POST, riceItem + "/check", ana, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.checked[0].food").value("Rice"));
    call(HttpMethod.POST, list + "/items", juan, "{\"food\":\"Rice\",\"grams\":100}")
        .andExpect(status().isCreated());
    call(HttpMethod.DELETE, riceItem + "/check", juan, "").andExpect(status().isConflict());
    call(HttpMethod.DELETE, list + "/checked", juan, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.checked.length()").value(0))
        .andExpect(jsonPath("$.pending.length()").value(2));
    call(HttpMethod.POST, list + "/items", juan, "{\"food\":\"Unicorn\",\"grams\":1}")
        .andExpect(status().isUnprocessableContent());
    call(HttpMethod.GET, list, signUp("outsider@haypacomer.dev"), "")
        .andExpect(status().isNotFound());
  }
}
