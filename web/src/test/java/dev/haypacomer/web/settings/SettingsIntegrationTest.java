package dev.haypacomer.web.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SettingsIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  @Autowired private MockMvc mvc;
  @Autowired private PolicySource policies;

  private String signUp(String email) throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\""
                        + email
                        + "\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}"))
        .andExpect(status().isCreated());
    String login =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + JsonPath.read(login, "$.accessToken");
  }

  @Test
  void ownersTuneTheirHouseholdWithinTheAllowedRange() throws Exception {
    String juan = signUp("settings@haypacomer.dev");
    String stranger = signUp("settings-stranger@haypacomer.dev");
    String created =
        mvc.perform(
                post("/api/v1/households")
                    .header("Authorization", juan)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = JsonPath.read(created, "$.id");
    String path = "/api/v1/households/" + id + "/settings";

    mvc.perform(get("/api/v1/settings/defaults").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(17));
    mvc.perform(get(path).header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(8))
        .andExpect(jsonPath("$[?(@.key == 'food.at-risk-days')].value").value("2"));

    mvc.perform(
            put(path)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"food.at-risk-days\",\"value\":\"4\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("4"))
        .andExpect(jsonPath("$.defaultValue").value("2"))
        .andExpect(jsonPath("$.overridden").value(true));
    assertEquals(new FreshnessPolicy(4), policies.freshness(new HouseholdId(UUID.fromString(id))));

    mvc.perform(
            put(path)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"food.at-risk-days\",\"value\":\"40\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("food.at-risk-days must be between 0 and 14"));
    mvc.perform(
            put(path)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"auth.refresh-days\",\"value\":\"10\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(path)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"food.at-risk-days\",\"value\":null}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("2"))
        .andExpect(jsonPath("$.overridden").value(false));

    mvc.perform(get(path).header("Authorization", stranger)).andExpect(status().isNotFound());
    mvc.perform(
            put(path)
                .header("Authorization", stranger)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"food.at-risk-days\",\"value\":\"3\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/settings/defaults")).andExpect(status().isUnauthorized());
  }
}
