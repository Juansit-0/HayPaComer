package dev.haypacomer.web.device;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.sensor.CheckFridgeAlerts;
import dev.haypacomer.sensors.esp32.Esp32Envelope;
import dev.haypacomer.sensors.esp32.Esp32Simulator;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
class SensorIntakeIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;
  @Autowired private CheckFridgeAlerts checkFridgeAlerts;

  private final Esp32Simulator simulator = new Esp32Simulator("fridge-01");

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

  private ResultActions send(String key, String payload) throws Exception {
    return mvc.perform(
        post("/api/v1/device/events")
            .header("X-Device-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload));
  }

  @Test
  void deviceEventsAreValidatedStoredOnceAndDriveAlertsAndColdChain() throws Exception {
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
    String key =
        JsonPath.read(
            json(
                base + "/devices",
                juan,
                "{\"fridgeId\":\""
                    + fridgeId
                    + "\",\"name\":\"Door\",\"kind\":\"ESP32_DOOR_TEMP\"}",
                201),
            "$.apiKey");

    Instant start = Instant.now().minus(Duration.ofMinutes(40)).truncatedTo(ChronoUnit.SECONDS);
    List<Esp32Envelope> batch = new ArrayList<>();
    batch.add(simulator.door(true, Instant.now().minusSeconds(90).truncatedTo(ChronoUnit.SECONDS)));
    batch.addAll(simulator.coldChainBreak(start, Duration.ofMinutes(10), "8", "9", "11", "10"));
    batch.add(simulator.temperature("75", Instant.now().truncatedTo(ChronoUnit.SECONDS)));
    String payload = simulator.toJson(batch);

    send(key, payload)
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.accepted").value(5))
        .andExpect(jsonPath("$.rejected").value(1))
        .andExpect(jsonPath("$.events[5].reason").value("tempC outside [-30, 60]"));
    send(key, simulator.toJson(batch.subList(0, 5)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duplicates").value(5));
    send(key, simulator.toJson(simulator.temperature("99", Instant.now())))
        .andExpect(status().isBadRequest());
    send(key, "{\"type\":\"DOOR\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Malformed sensor payload"));
    send("hpc_dev_wrong", payload).andExpect(status().isUnauthorized());

    mvc.perform(get(base + "/cold-chain").header("Authorization", juan))
        .andExpect(jsonPath("$[0].phase").value("UNDER_REVIEW"));
    mvc.perform(get(base + "/cold-investigation?hours=2").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].readings").value(4))
        .andExpect(jsonPath("$[0].episodes[0].cause").value("DOOR_LEFT_OPEN"))
        .andExpect(jsonPath("$[0].episodes[0].peakCelsius").value(11))
        .andExpect(jsonPath("$[0].episodes[0].end").doesNotExist());
    mvc.perform(get(base + "/cold-investigation?hours=200").header("Authorization", juan))
        .andExpect(status().isBadRequest());
    checkFridgeAlerts.check();
    mvc.perform(get("/api/v1/device/commands").header("X-Device-Key", key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasItems("DOOR_OPEN_BEEP", "COLD_CHAIN_ALARM")));

    String inbox =
        mvc.perform(get("/api/v1/notifications").header("Authorization", juan))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].type", hasItems("DOOR_LEFT_OPEN", "COLD_CHAIN_BREACH")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String first = JsonPath.read(inbox, "$[0].id");
    mvc.perform(patch("/api/v1/notifications/" + first + "/read").header("Authorization", juan))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/notifications").header("Authorization", juan))
        .andExpect(jsonPath("$[0].readAt").exists());
    mvc.perform(
            patch("/api/v1/notifications/" + UUID.randomUUID() + "/read")
                .header("Authorization", juan))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/me/notification-preferences").header("Authorization", juan))
        .andExpect(jsonPath("$.channels", hasItems("WEB", "LOG")));
    mvc.perform(
            put("/api/v1/me/notification-preferences")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channels\":[\"WEB\",\"TELEGRAM\"],\"telegramChatId\":\"12345\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.telegramChatId").value("12345"));
    mvc.perform(
            put("/api/v1/me/notification-preferences")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channels\":[\"TELEGRAM\"]}"))
        .andExpect(status().isBadRequest());
  }
}
