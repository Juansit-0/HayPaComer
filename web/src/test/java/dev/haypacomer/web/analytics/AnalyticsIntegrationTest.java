package dev.haypacomer.web.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
class AnalyticsIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  @Autowired private MockMvc mvc;

  private ResultActions send(String path, String bearer, String body) throws Exception {
    var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      request.header("Authorization", bearer);
    }
    return mvc.perform(request);
  }

  private static String body(ResultActions result) throws Exception {
    return result.andReturn().getResponse().getContentAsString();
  }

  private String signUp(String email) throws Exception {
    send(
            "/api/v1/auth/register",
            null,
            "{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}")
        .andExpect(status().isCreated());
    return "Bearer "
        + JsonPath.read(
            body(
                send(
                    "/api/v1/auth/login",
                    null,
                    "{\"email\":\"" + email + "\",\"password\":\"fresh-milk-842\"}")),
            "$.accessToken");
  }

  @Test
  void rescuedFoodAndWasteBecomeGramsAndPesos() throws Exception {
    String juan = signUp("analytics@haypacomer.dev");
    String stranger = signUp("analytics-stranger@haypacomer.dev");
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
    String place =
        "{\"fridgeId\":\""
            + JsonPath.read(fridge, "$.id")
            + "\",\"trayId\":\""
            + JsonPath.read(fridge, "$.children[0].children[0].id")
            + "\",";
    String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();
    String chicken =
        JsonPath.read(
            body(
                send(
                    base + "/items",
                    juan,
                    place
                        + "\"food\":\"Chicken breast\",\"grams\":650,\"expiresOn\":\""
                        + tomorrow
                        + "\"}")),
            "$.itemId");
    String rice =
        JsonPath.read(
            body(send(base + "/items", juan, place + "\"food\":\"Rice\",\"grams\":500}")),
            "$.itemId");
    send(base + "/items/" + chicken + "/consume", juan, "{\"grams\":250}")
        .andExpect(status().isOk());
    send(base + "/items/" + rice + "/discard", juan, "{}").andExpect(status().isOk());

    mvc.perform(get(base + "/analytics").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("COP"))
        .andExpect(jsonPath("$.days.length()").value(30))
        .andExpect(jsonPath("$.total.rescuedGrams").value(250))
        .andExpect(jsonPath("$.total.discardedGrams").value(500))
        .andExpect(jsonPath("$.moneySaved").value(5500))
        .andExpect(jsonPath("$.moneyWasted").value(2400))
        .andExpect(jsonPath("$.foods[0].foodKey").value("chicken breast"));

    mvc.perform(
            put(base + "/prices")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"food\":\"Chicken breast\",\"pricePerKg\":30000}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.foodKey").value("chicken breast"));
    mvc.perform(get(base + "/analytics").header("Authorization", juan))
        .andExpect(jsonPath("$.moneySaved").value(7500));
    mvc.perform(get(base + "/prices").header("Authorization", juan))
        .andExpect(jsonPath("$['chicken breast']").value(30000));

    mvc.perform(
            put(base + "/prices")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"food\":\"Caviar\",\"pricePerKg\":30000}"))
        .andExpect(status().isUnprocessableContent());
    mvc.perform(
            get(base + "/analytics?from=2026-01-10&to=2026-01-01").header("Authorization", juan))
        .andExpect(status().isBadRequest());
    mvc.perform(get(base + "/analytics").header("Authorization", stranger))
        .andExpect(status().isNotFound());

    mvc.perform(get(base + "/analytics/report?format=csv").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.startsWith("attachment; filename=\"haypacomer-")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString("food,chicken breast,250,250,0,0.000")));
    mvc.perform(get(base + "/analytics/report").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.startsWith("# Apartment kitchen report")));
    mvc.perform(get(base + "/analytics/report?format=pdf").header("Authorization", juan))
        .andExpect(status().isBadRequest());
    mvc.perform(get(base + "/analytics/report").header("Authorization", stranger))
        .andExpect(status().isNotFound());
  }
}
