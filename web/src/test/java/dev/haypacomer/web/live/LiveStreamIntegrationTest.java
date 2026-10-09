package dev.haypacomer.web.live;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.domain.household.HouseholdId;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = "haypacomer.security.jwt.secret=integration-secret-with-at-least-32-bytes")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class LiveStreamIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;
  @Autowired private LiveStreamHub hub;

  private final Esp32Simulator simulator = new Esp32Simulator("door-01");

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

  private String signIn(String email) throws Exception {
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
  void streamsInventorySensorAndTwinUpdatesToMembersOnly() throws Exception {
    String juan = signIn("juan@haypacomer.dev");
    String householdId =
        JsonPath.read(
            body(
                send(
                    "/api/v1/households",
                    juan,
                    "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}")),
            "$.id");
    String base = "/api/v1/households/" + householdId;
    String fridge = body(send(base + "/fridges", juan, "{\"name\":\"Kitchen\"}"));
    String fridgeId = JsonPath.read(fridge, "$.id");
    String top = JsonPath.read(fridge, "$.children[0].children[0].id");

    mvc.perform(get(base + "/fridges/" + fridgeId + "/twin").header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doorOpen").doesNotExist())
        .andExpect(jsonPath("$.celsius").doesNotExist());

    MvcResult stream =
        mvc.perform(get(base + "/stream").header("Authorization", juan))
            .andExpect(status().isOk())
            .andReturn();
    assertTrue(stream.getRequest().isAsyncStarted());
    assertEquals(1, hub.listeners(new HouseholdId(UUID.fromString(householdId))));

    send(
            base + "/items",
            juan,
            "{\"fridgeId\":\""
                + fridgeId
                + "\",\"trayId\":\""
                + top
                + "\",\"food\":\"Milk\",\"grams\":842}")
        .andExpect(status().isCreated());

    String key =
        JsonPath.read(
            body(
                send(
                    base + "/devices",
                    juan,
                    "{\"fridgeId\":\""
                        + fridgeId
                        + "\",\"name\":\"Door\",\"kind\":\"ESP32_DOOR_TEMP\"}")),
            "$.apiKey");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    mvc.perform(
            post("/api/v1/device/events")
                .header("X-Device-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    simulator.toJson(
                        List.of(
                            simulator.door(true, now.minusSeconds(5)),
                            simulator.temperature("4.2", now)))))
        .andExpect(status().isAccepted());

    String scaleBody =
        body(
            send(
                base + "/devices",
                juan,
                "{\"fridgeId\":\"" + fridgeId + "\",\"name\":\"Counter\",\"kind\":\"SIMULATOR\"}"));
    mvc.perform(
            put(base + "/devices/" + JsonPath.read(scaleBody, "$.device.id") + "/scale/mode")
                .header("Authorization", juan)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"COOKING\",\"food\":\"Rice\",\"targetGrams\":150}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/device/events")
                .header("X-Device-Key", (String) JsonPath.read(scaleBody, "$.apiKey"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new Esp32Simulator("scale-01")
                        .toJson(
                            List.of(
                                new Esp32Simulator("scale-01")
                                    .weight("60", true, "COOKING", now.plusSeconds(1))))))
        .andExpect(status().isAccepted());

    String events = stream.getResponse().getContentAsString();
    assertTrue(events.contains("event:copilot"));
    assertTrue(events.contains("Add 90 g more Rice (40%)."));
    assertTrue(events.contains("event:ready"));
    assertTrue(events.contains("event:inventory"));
    assertTrue(events.contains("STOCK_FOOD 842.00 g left"));
    assertTrue(events.contains("event:sensor"));
    assertTrue(events.contains("door open"));
    assertTrue(events.contains("4.2 C"));

    mvc.perform(get(base + "/fridges/" + fridgeId + "/twin").header("Authorization", juan))
        .andExpect(jsonPath("$.doorOpen").value(true))
        .andExpect(jsonPath("$.celsius").value(4.2));

    String ana = signIn("ana@haypacomer.dev");
    mvc.perform(get(base + "/stream").header("Authorization", ana))
        .andExpect(status().isNotFound());
    mvc.perform(get(base + "/stream")).andExpect(status().isUnauthorized());
    mvc.perform(get(base + "/fridges/" + UUID.randomUUID() + "/twin").header("Authorization", juan))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail", containsString("Fridge")));
  }
}
