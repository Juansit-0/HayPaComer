package dev.haypacomer.web.kitchen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SuggestionIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  @Autowired private MockMvc mvc;

  private ResultActions call(String path, String bearer, String body) throws Exception {
    var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      request.header("Authorization", bearer);
    }
    return mvc.perform(request);
  }

  private String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  private static String candidate(String name, int minutes, String requirements) {
    return "{\"name\":\""
        + name
        + "\",\"servings\":2,\"minutes\":"
        + minutes
        + ",\"requirements\":["
        + requirements
        + "]}";
  }

  @Test
  void suggestsRescueFirstDishesFromTheRealFridge() throws Exception {
    call(
            "/api/v1/auth/register",
            null,
            "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}")
        .andExpect(status().isCreated());
    String juan =
        "Bearer "
            + JsonPath.read(
                body(
                    call(
                        "/api/v1/auth/login",
                        null,
                        "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}")),
                "$.accessToken");
    String base =
        "/api/v1/households/"
            + JsonPath.read(
                body(
                    call(
                        "/api/v1/households",
                        juan,
                        "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}")),
                "$.id");
    String fridge = body(call(base + "/fridges", juan, "{\"name\":\"Kitchen\"}"));
    String fridgeId = JsonPath.read(fridge, "$.id");
    String top = JsonPath.read(fridge, "$.children[0].children[0].id");
    String tomorrow = LocalDate.now().plusDays(1).toString();
    for (String item :
        new String[] {
          "\"food\":\"Chicken breast\",\"grams\":250,\"expiresOn\":\"" + tomorrow + "\"",
          "\"food\":\"Rice\",\"grams\":500"
        }) {
      call(
              base + "/items",
              juan,
              "{\"fridgeId\":\"" + fridgeId + "\",\"trayId\":\"" + top + "\"," + item + "}")
          .andExpect(status().isCreated());
    }
    String request =
        "{\"servings\":2,\"candidates\":["
            + candidate("Plain rice", 15, "{\"food\":\"Rice\",\"grams\":150}")
            + ","
            + candidate(
                "Rice with chicken",
                35,
                "{\"food\":\"Chicken breast\",\"grams\":200},{\"food\":\"Rice\",\"grams\":150}")
            + ","
            + candidate("Tuna salad", 10, "{\"food\":\"Tuna\",\"grams\":300}")
            + "]}";

    call(base + "/suggestions", juan, request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("OFFLINE_RULES"))
        .andExpect(jsonPath("$.suggestions.length()").value(2))
        .andExpect(jsonPath("$.suggestions[0].recipe").value("Rice with chicken"))
        .andExpect(jsonPath("$.suggestions[0].rescuedFoods[0]").value("Chicken breast"))
        .andExpect(jsonPath("$.suggestions[0].evaluation.verdict").value("ENOUGH"));
    call(base + "/rescue", juan, request)
        .andExpect(jsonPath("$.suggestions.length()").value(1))
        .andExpect(jsonPath("$.suggestions[0].recipe").value("Rice with chicken"));
    call(base + "/suggestions", juan, "{\"candidates\":[]}").andExpect(status().isBadRequest());
    call(base + "/suggestions", null, request).andExpect(status().isUnauthorized());
  }
}
