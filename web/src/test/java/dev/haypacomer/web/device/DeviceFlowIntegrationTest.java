package dev.haypacomer.web.device;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.HouseholdId;
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
class DeviceFlowIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MockMvc mvc;
  @Autowired private FridgeRepository fridges;

  private String send(String path, String bearer, String body, int expected) throws Exception {
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
  void ownerRegistersADeviceThatThenAuthenticatesUntilRevoked() throws Exception {
    send(
        "/api/v1/auth/register",
        null,
        "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\",\"displayName\":\"J\"}",
        201);
    String login =
        send(
            "/api/v1/auth/login",
            null,
            "{\"email\":\"juan@haypacomer.dev\",\"password\":\"fresh-milk-842\"}",
            200);
    String juan = "Bearer " + JsonPath.read(login, "$.accessToken");
    String household =
        send(
            "/api/v1/households",
            juan,
            "{\"name\":\"Apartment\",\"currency\":\"COP\",\"timezone\":\"UTC\"}",
            201);
    UUID householdId = UUID.fromString(JsonPath.read(household, "$.id"));
    Fridge fridge = Fridge.named("Kitchen fridge");
    fridges.save(new HouseholdId(householdId), fridge);
    String path = "/api/v1/households/" + householdId + "/devices";

    String registered =
        send(
            path,
            juan,
            "{\"fridgeId\":\""
                + fridge.id().value()
                + "\",\"name\":\"Door\",\"kind\":\"ESP32_DOOR_TEMP\"}",
            201);
    String key = JsonPath.read(registered, "$.apiKey");
    String deviceId = JsonPath.read(registered, "$.device.id");

    mvc.perform(get("/api/v1/device/whoami").header("X-Device-Key", key))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fridgeId").value(fridge.id().value().toString()));
    mvc.perform(get(path).header("Authorization", juan))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].lastSeenAt").isNotEmpty());
    mvc.perform(get("/api/v1/device/whoami").header("X-Device-Key", key + "x"))
        .andExpect(status().isUnauthorized());

    mvc.perform(delete(path + "/" + deviceId).header("Authorization", juan))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/device/whoami").header("X-Device-Key", key))
        .andExpect(status().isUnauthorized());
  }
}
