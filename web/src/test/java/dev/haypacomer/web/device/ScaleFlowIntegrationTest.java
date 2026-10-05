package dev.haypacomer.web.device;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.sensors.esp32.Esp32Simulator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
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
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ScaleFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;

  private final Esp32Simulator simulator = new Esp32Simulator("scale-01");

  private String json(String path, String bearer, String body, int expected) throws Exception {
    var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (bearer != null) {
      request.header("Authorization", bearer);
    }
    return mvc.perform(request)
        .andExpect(status().is(expected))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private void sample(String key, long counts, Instant at) throws Exception {
    mvc.perform(
            post("/api/v1/device/events")
                .header("X-Device-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(simulator.toJson(List.of(simulator.rawWeight(counts, "FRIDGE", at)))))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void tareCalibrateAndReadThroughRawSamples() throws Exception {
    json(
        "/api/v1/auth/register",
        null,
        "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}",
        201);
    String juan =
        "Bearer "
            + JsonPath.read(
                json(
                    "/api/v1/auth/login",
                    null,
                    "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}",
                    200),
                "$.accessToken");
    String base =
        "/api/v1/households/"
            + JsonPath.read(
                json(
                    "/api/v1/households",
                    juan,
                    "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}",
                    201),
                "$.id");
    String fridgeId =
        JsonPath.read(json(base + "/fridges", juan, "{\"name\":\"Kitchen\"}", 201), "$.id");
    String registered =
        json(
            base + "/devices",
            juan,
            "{\"fridgeId\":\"" + fridgeId + "\",\"name\":\"Scale\",\"kind\":\"ESP32_SCALE\"}",
            201);
    String key = JsonPath.read(registered, "$.apiKey");
    String scale = base + "/devices/" + JsonPath.read(registered, "$.device.id") + "/scale";

    json(scale + "/tare", juan, "", 409);
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    sample(key, 84_000, now.minusSeconds(3));
    json(scale + "/tare", juan, "", 200);
    sample(key, 298_000, now.minusSeconds(2));
    mvc.perform(
            post(scale + "/calibrate")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"knownGrams\":500}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.calibrated").value(true));
    sample(key, 84_000 + 428L * 842, now.minusSeconds(1));

    mvc.perform(get(scale + "/reading").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.grams").value(842.0))
        .andExpect(jsonPath("$.calibrated").value(true));
    json(base + "/devices/" + UUID.randomUUID() + "/scale/tare", juan, "", 404);

    String rack =
        JsonPath.read(
            json(base + "/fridges", juan, "{\"name\":\"Garage\"}", 201),
            "$.children[1].children[0].id");
    String garage =
        JsonPath.read(
            mvc.perform(get(base + "/fridges").header("Authorization", juan))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$[0].id");
    String milk =
        JsonPath.read(
            json(
                base + "/items",
                juan,
                "{\"fridgeId\":\""
                    + garage
                    + "\",\"trayId\":\""
                    + rack
                    + "\",\"food\":\"Milk\",\"grams\":892,\"tareGrams\":50}",
                201),
            "$.itemId");
    mvc.perform(
            put(scale + "/item")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\":\"" + milk + "\"}"))
        .andExpect(status().isOk());
    sample(key, 84_000 + 428L * 700, now);
    sample(key, 84_000 + 428L * 700, now.plusSeconds(2));

    mvc.perform(get(base + "/inventory").header("Authorization", juan))
        .andExpect(jsonPath("$[0].name").value("Milk"))
        .andExpect(jsonPath("$[0].grams").value(650.0));
    mvc.perform(get(base + "/activity").header("Authorization", juan))
        .andExpect(jsonPath("$[0].action").value("CONSUME_FOOD"))
        .andExpect(jsonPath("$[0].detail.source").value("SCALE"));
    mvc.perform(delete(scale + "/item").header("Authorization", juan))
        .andExpect(status().isNoContent());
  }
}
