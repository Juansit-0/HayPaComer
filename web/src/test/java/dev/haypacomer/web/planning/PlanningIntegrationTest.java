package dev.haypacomer.web.planning;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
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
class PlanningIntegrationTest {

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
  void savesRecipesAndPlansARescueFirstWeek() throws Exception {
    send(
            "/api/v1/auth/register",
            null,
            "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}")
        .andExpect(status().isCreated());
    String juan =
        "Bearer "
            + JsonPath.read(
                body(
                    send(
                        "/api/v1/auth/login",
                        null,
                        "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}")),
                "$.accessToken");
    String base =
        "/api/v1/households/"
            + JsonPath.read(
                body(
                    send(
                        "/api/v1/households",
                        juan,
                        "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}")),
                "$.id");
    String fridge = body(send(base + "/fridges", juan, "{\"name\":\"Kitchen\"}"));
    String fridgeId = JsonPath.read(fridge, "$.id");
    String top = JsonPath.read(fridge, "$.children[0].children[0].id");
    send(
            base + "/items",
            juan,
            "{\"fridgeId\":\""
                + fridgeId
                + "\",\"trayId\":\""
                + top
                + "\",\"food\":\"Chicken breast\",\"grams\":200,\"expiresOn\":\""
                + LocalDate.now().plusDays(1)
                + "\"}")
        .andExpect(status().isCreated());

    mvc.perform(get(base + "/weekly-plans/current").header("Authorization", juan))
        .andExpect(status().isNotFound());
    String stew =
        body(
            send(
                    base + "/recipes",
                    juan,
                    "{\"name\":\"Lentil stew\",\"servings\":2,\"minutes\":40,"
                        + "\"requirements\":[{\"food\":\"Lentils\",\"quantity\":\"200 g\"}],"
                        + "\"steps\":[{\"instruction\":\"Simmer\",\"timerSeconds\":1800}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.steps[0].timerSeconds").value(1800)));
    send(
            base + "/recipes",
            juan,
            "{\"name\":\"Chicken bowl\",\"servings\":2,\"minutes\":25,"
                + "\"requirements\":[{\"food\":\"Chicken breast\",\"grams\":200}]}")
        .andExpect(status().isCreated());
    mvc.perform(get(base + "/recipes").header("Authorization", juan))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Chicken bowl"));

    String plan =
        body(
            send(base + "/weekly-plans", juan, "{\"servings\":2}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entries.length()").value(14))
                .andExpect(jsonPath("$.entries[0].recipe").value("Chicken bowl"))
                .andExpect(jsonPath("$.entries[0].needsShopping").value(false))
                .andExpect(jsonPath("$.entries[0].date").value(LocalDate.now().toString())));
    String entry = JsonPath.read(plan, "$.entries[0].id");
    mvc.perform(
            patch(base + "/plan-entries/" + entry)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":\"" + JsonPath.read(stew, "$.id") + "\",\"servings\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recipe").value("Lentil stew"))
        .andExpect(jsonPath("$.needsShopping").value(true));
    mvc.perform(get(base + "/weekly-plans/current").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entries[0].servings").value(3));
    mvc.perform(
            patch(base + "/plan-entries/" + entry)
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":\"" + UUID.randomUUID() + "\",\"servings\":2}"))
        .andExpect(status().isNotFound());
    send(base + "/weekly-plans", null, "{}").andExpect(status().isUnauthorized());
  }
}
