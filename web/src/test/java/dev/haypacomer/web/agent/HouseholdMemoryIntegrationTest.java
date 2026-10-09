package dev.haypacomer.web.agent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class HouseholdMemoryIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  @Autowired private MockMvc mvc;

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
  void theHouseholdEditsWhatTheAgentRemembers() throws Exception {
    String juan = signUp("memory@haypacomer.dev");
    String stranger = signUp("stranger@haypacomer.dev");
    String household =
        mvc.perform(
                post("/api/v1/households")
                    .header("Authorization", juan)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String memory = "/api/v1/households/" + JsonPath.read(household, "$.id") + "/agent/memory";

    mvc.perform(
            patch(memory)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"remember\":[{\"topic\":\"USUAL_QUANTITY\",\"subject\":\"Rice\",\"value\":\"150 g\"},"
                        + "{\"topic\":\"PREFERENCE\",\"subject\":\"spicy food\",\"value\":\"mild\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].topic").value("PREFERENCE"))
        .andExpect(jsonPath("$[1].subject").value("rice"));

    mvc.perform(
            patch(memory)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"forget\":[{\"topic\":\"PREFERENCE\",\"subject\":\"Spicy food\"}]}"))
        .andExpect(jsonPath("$.length()").value(1));

    mvc.perform(
            patch(memory)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"remember\":[{\"topic\":\"USUAL_QUANTITY\",\"subject\":\"beans\",\"value\":\"a lot\"}]}"))
        .andExpect(status().isUnprocessableContent());
    mvc.perform(
            patch(memory)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"remember\":[{\"topic\":\"DECISION\",\"subject\":\"x\"}]}"))
        .andExpect(status().isBadRequest());

    mvc.perform(get(memory).header("Authorization", stranger)).andExpect(status().isNotFound());
    mvc.perform(get(memory)).andExpect(status().isUnauthorized());

    mvc.perform(delete(memory).header("Authorization", juan)).andExpect(status().isNoContent());
    mvc.perform(get(memory).header("Authorization", juan))
        .andExpect(jsonPath("$.length()").value(0));
  }
}
