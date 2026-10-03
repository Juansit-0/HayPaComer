package dev.haypacomer.web.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes",
    })
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;

  private String send(String path, String body, int expectedStatus) throws Exception {
    return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().is(expectedStatus))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void registerLoginRefreshReuseAndLogout() throws Exception {
    send(
        "/api/v1/auth/register",
        "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"Juan\"}",
        201);
    send(
        "/api/v1/auth/register",
        "{\"email\":\"JUAN@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}",
        409);

    String login =
        send(
            "/api/v1/auth/login",
            "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}",
            200);
    String access = JsonPath.read(login, "$.accessToken");
    String refresh = JsonPath.read(login, "$.refreshToken");

    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("juan@haypacomer.dev"));

    String rotated = send("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}", 200);
    String newRefresh = JsonPath.read(rotated, "$.refreshToken");

    send("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}", 401);
    send("/api/v1/auth/refresh", "{\"refreshToken\":\"" + newRefresh + "\"}", 401);

    String again =
        send(
            "/api/v1/auth/login",
            "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}",
            200);
    String lastRefresh = JsonPath.read(again, "$.refreshToken");
    send("/api/v1/auth/logout", "{\"refreshToken\":\"" + lastRefresh + "\"}", 204);
    send("/api/v1/auth/refresh", "{\"refreshToken\":\"" + lastRefresh + "\"}", 401);
  }

  @Test
  void locksOutAfterFiveFailures() throws Exception {
    send(
        "/api/v1/auth/register",
        "{\"email\":\"ana@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"Ana\"}",
        201);
    for (int attempt = 0; attempt < 5; attempt++) {
      send(
          "/api/v1/auth/login",
          "{\"email\":\"ana@haypacomer.dev\",\"password\":\"wrong-password\"}",
          401);
    }

    send(
        "/api/v1/auth/login",
        "{\"email\":\"ana@haypacomer.dev\",\"password\":\"fresh-milk-842\"}",
        429);
  }
}
