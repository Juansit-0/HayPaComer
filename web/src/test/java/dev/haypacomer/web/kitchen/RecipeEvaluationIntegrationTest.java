package dev.haypacomer.web.kitchen;

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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class RecipeEvaluationIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

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

  private String recipe(String strategy, String substitutes) {
    return "{\"name\":\"Rice with chicken\",\"servings\":2,\"minutes\":35,\"targetServings\":2,"
        + "\"requirements\":[{\"food\":\"Chicken breast\",\"grams\":200},{\"food\":\"Rice\",\"quantity\":\"0,15 kg\"}],"
        + "\"strategy\":\""
        + strategy
        + "\""
        + substitutes
        + "}";
  }

  @Test
  void evaluatesTheDemoRecipeAgainstTheRealInventory() throws Exception {
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
    for (String[] item :
        new String[][] {
          {"Chicken breast", "80"}, {"Rice", "300"}, {"Tuna", "130"}, {"Tuna", "130"}
        }) {
      call(
              base + "/items",
              juan,
              "{\"fridgeId\":\""
                  + fridgeId
                  + "\",\"trayId\":\""
                  + top
                  + "\",\"food\":\""
                  + item[0]
                  + "\",\"grams\":"
                  + item[1]
                  + "}")
          .andExpect(status().isCreated());
    }
    String evaluate = base + "/recipes/evaluate";

    call(evaluate, juan, recipe("STRICT", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verdict").value("MISSING"))
        .andExpect(jsonPath("$.requirements[0].shortfallGrams").value(120.0));
    call(evaluate, juan, recipe("FLEXIBLE", ""))
        .andExpect(jsonPath("$.verdict").value("REDUCE"))
        .andExpect(jsonPath("$.achievableServings").value(1));
    call(evaluate, juan, recipe("RESCUE", ",\"substitutes\":{\"chicken breast\":[\"Tuna\"]}"))
        .andExpect(jsonPath("$.verdict").value("SUBSTITUTE"))
        .andExpect(jsonPath("$.requirements[0].substitute").value("Tuna"));
    call(evaluate, juan, recipe("STRICT", "").replace("Rice\"", "Unicorn\""))
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void interpretsQuantitiesWithTheCatalogFactors() throws Exception {
    call(
            "/api/v1/auth/register",
            null,
            "{\"email\":\"ana@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"A\"}")
        .andExpect(status().isCreated());
    String ana =
        "Bearer "
            + JsonPath.read(
                body(
                    call(
                        "/api/v1/auth/login",
                        null,
                        "{\"email\":\"ana@haypacomer.dev\",\"password\":\"fresh-milk-842\"}")),
                "$.accessToken");
    String interpret = "/api/v1/foods/interpret";

    call(interpret, ana, "{\"food\":\"Milk\",\"quantity\":\"2 tazas de leche\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.food").value("Milk"))
        .andExpect(jsonPath("$.expression").value("2 cup"))
        .andExpect(jsonPath("$.grams").value(494.4));
    call(interpret, ana, "{\"food\":\"egg\",\"quantity\":\"3 huevos + 1/2 kg\"}")
        .andExpect(jsonPath("$.grams").value(650.0));
    call(interpret, ana, "{\"food\":\"Rice\",\"quantity\":\"1 cup\"}")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.title").value("Quantity cannot be weighed"));
    call(interpret, ana, "{\"food\":\"Rice\",\"quantity\":\"a handful\"}")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.title").value("Unreadable quantity"));
    call(interpret, null, "{\"food\":\"Rice\",\"quantity\":\"1 g\"}")
        .andExpect(status().isUnauthorized());
  }
}
