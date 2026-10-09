package dev.haypacomer.web.kitchen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ExpiryIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  @ServiceConnection(name = "redis")
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  @Autowired private MockMvc mvc;

  private String body(String path, String bearer, String json) throws Exception {
    return mvc.perform(
            post(path)
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void unknownDatesAreEstimatedImpossibleOnesRejectedAndOpeningShortensTheDate() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"expiry@haypacomer.dev\",\"password\":\"fresh-milk-842\","
                        + "\"displayName\":\"J\"}"))
        .andExpect(status().isCreated());
    String login =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"expiry@haypacomer.dev\",\"password\":\"fresh-milk-842\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String juan = "Bearer " + JsonPath.read(login, "$.accessToken");
    String base =
        "/api/v1/households/"
            + JsonPath.read(
                body(
                    "/api/v1/households",
                    juan,
                    "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}"),
                "$.id");
    String fridge = body(base + "/fridges", juan, "{\"name\":\"Kitchen\"}");
    String place =
        "{\"fridgeId\":\""
            + JsonPath.read(fridge, "$.id")
            + "\",\"trayId\":\""
            + JsonPath.read(fridge, "$.children[0].children[0].id")
            + "\",";
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    String chicken =
        body(base + "/items", juan, place + "\"food\":\"Chicken breast\",\"grams\":500}");
    mvc.perform(get(base + "/inventory").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].expiresOn").value(today.plusDays(2).toString()))
        .andExpect(jsonPath("$[0].expirySource").value("ESTIMATED"))
        .andExpect(jsonPath("$[0].expiryConfidence").value(0.6));

    mvc.perform(
            post(base + "/items")
                .header("Authorization", juan)
                .header("Accept-Language", "es-CO")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    place
                        + "\"food\":\"Milk\",\"grams\":900,\"expiresOn\":\""
                        + today.plusDays(40)
                        + "\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.title").value("Fecha de vencimiento imposible"))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Milk dura unos 7 días en la nevera, así que una fecha a 40 días no es"
                        + " posible; revisa la etiqueta o deja que HayPaComer la estime"));
    String milk =
        body(
            base + "/items",
            juan,
            place + "\"food\":\"Milk\",\"grams\":900,\"expiresOn\":\"" + today.plusDays(6) + "\"}");

    mvc.perform(
            get(base + "/expiry-estimate")
                .header("Authorization", juan)
                .param("food", "Milk")
                .param("zone", "DOOR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expiresOn").value(today.plusDays(5).toString()))
        .andExpect(jsonPath("$.source").value("ESTIMATED"))
        .andExpect(jsonPath("$.shelfDays").value(5));
    mvc.perform(
            post(base + "/items/" + JsonPath.read(milk, "$.itemId") + "/open")
                .header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expiresOn").value(today.plusDays(4).toString()))
        .andExpect(jsonPath("$.openedOn").value(today.toString()))
        .andExpect(jsonPath("$.expirySource").value("ESTIMATED"));
    mvc.perform(
            post(base + "/items/" + JsonPath.read(chicken, "$.itemId") + "/open")
                .header("Authorization", juan))
        .andExpect(jsonPath("$.expiresOn").value(today.plusDays(2).toString()));
    mvc.perform(
            get(base + "/expiry-estimate").header("Authorization", juan).param("food", "Unicorn"))
        .andExpect(status().isUnprocessableContent());
  }
}
