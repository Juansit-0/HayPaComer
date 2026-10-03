package dev.haypacomer.web.kitchen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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
class ColdChainFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;
  @Autowired private TrackColdChain trackColdChain;

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

  @Test
  void breachIsShownUntilReviewedAfterRecovery() throws Exception {
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
    String householdId =
        JsonPath.read(
            json(
                "/api/v1/households",
                juan,
                "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}",
                201),
            "$.id");
    String base = "/api/v1/households/" + householdId;
    String fridge = json(base + "/fridges", juan, "{\"name\":\"Kitchen\"}", 201);
    UUID fridgeId = UUID.fromString(JsonPath.read(fridge, "$.id"));
    String rack = JsonPath.read(fridge, "$.children[1].children[0].id");
    json(
        base + "/items",
        juan,
        "{\"fridgeId\":\""
            + fridgeId
            + "\",\"trayId\":\""
            + rack
            + "\",\"food\":\"Chicken breast\",\"grams\":200}",
        201);
    Device probe =
        Device.register(
            new HouseholdId(UUID.fromString(householdId)),
            new FridgeId(fridgeId),
            "Probe",
            DeviceKind.SIMULATOR,
            "ab",
            Instant.now());
    Instant start = Instant.now().minus(Duration.ofHours(1));
    for (String[] reading : new String[][] {{"8", "0"}, {"10.5", "25"}, {"4", "40"}}) {
      trackColdChain.record(
          probe,
          new TemperatureReading(
              new SensorEventId(UUID.randomUUID()),
              probe.id(),
              probe.fridge(),
              start.plus(Duration.ofMinutes(Long.parseLong(reading[1]))),
              new BigDecimal(reading[0])));
    }

    mvc.perform(get(base + "/cold-chain").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].phase").value("UNDER_REVIEW"))
        .andExpect(jsonPath("$[0].recovered").value(true))
        .andExpect(jsonPath("$[0].peakCelsius").value(10.5));
    mvc.perform(get(base + "/inventory").header("Authorization", juan))
        .andExpect(jsonPath("$[0].statuses[0]").value("UNDER_REVIEW"))
        .andExpect(jsonPath("$[0].edible").value(false));

    mvc.perform(
            post(base + "/fridges/" + fridgeId + "/cold-chain/review")
                .header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.peakCelsius").value(10.5));
    mvc.perform(
            post(base + "/fridges/" + fridgeId + "/cold-chain/review")
                .header("Authorization", juan))
        .andExpect(status().isConflict());
    mvc.perform(
            post(base + "/fridges/" + UUID.randomUUID() + "/cold-chain/review")
                .header("Authorization", juan))
        .andExpect(status().isNotFound());
    mvc.perform(get(base + "/inventory").header("Authorization", juan))
        .andExpect(jsonPath("$[0].edible").value(true));
  }
}
