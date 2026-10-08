package dev.haypacomer.web.kitchen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class FridgeSessionIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;

  private ResultActions send(String path, String bearer, String body) throws Exception {
    var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      request.header("Authorization", bearer);
    }
    return mvc.perform(request);
  }

  private String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  @Test
  void oneMemberUsesTheFridgeAtATime() throws Exception {
    send(
            "/api/v1/auth/register",
            null,
            "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}")
        .andExpect(status().isCreated());
    String login =
        body(
            send(
                "/api/v1/auth/login",
                null,
                "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}"));
    String juan = "Bearer " + JsonPath.read(login, "$.accessToken");
    String base =
        "/api/v1/households/"
            + JsonPath.read(
                body(
                    send(
                        "/api/v1/households",
                        juan,
                        "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}")),
                "$.id");
    String fridgeId =
        JsonPath.read(body(send(base + "/fridges", juan, "{\"name\":\"Kitchen\"}")), "$.id");
    String session = base + "/fridges/" + fridgeId + "/session";

    mvc.perform(get(session).header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").doesNotExist());
    send(session, juan, "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").exists())
        .andExpect(jsonPath("$.expiresAt").exists());
    mvc.perform(delete(session).header("Authorization", juan))
        .andExpect(jsonPath("$.userId").doesNotExist());
    mvc.perform(
            get(base + "/fridges/" + UUID.randomUUID() + "/session").header("Authorization", juan))
        .andExpect(status().isNotFound());
    mvc.perform(get(session)).andExpect(status().isUnauthorized());
  }
}
