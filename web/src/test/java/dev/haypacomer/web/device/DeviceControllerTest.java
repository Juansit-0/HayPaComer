package dev.haypacomer.web.device;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.haypacomer.application.device.AuthenticateDevice;
import dev.haypacomer.application.device.InvalidDeviceKeyException;
import dev.haypacomer.application.device.ListDevices;
import dev.haypacomer.application.device.RegisterDevice;
import dev.haypacomer.application.device.RegisteredDevice;
import dev.haypacomer.application.device.RevokeDevice;
import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.application.sensor.IngestSensorEvents;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.sensors.hardware.QueuedAlertSignal;
import dev.haypacomer.web.error.ApiExceptionHandler;
import dev.haypacomer.web.security.JwtAccessTokenIssuer;
import dev.haypacomer.web.security.JwtProperties;
import dev.haypacomer.web.security.SecurityConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {DeviceController.class, DeviceSelfController.class})
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(
    properties = {
      "haypacomer.security.jwt.secret=test-secret-with-at-least-32-bytes!!",
      "haypacomer.security.jwt.issuer=https://api.haypacomer.dev",
      "haypacomer.security.jwt.access-token-time-to-live=15m"
    })
class DeviceControllerTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  @Autowired private MockMvc mvc;
  @Autowired private JwtEncoder encoder;
  @Autowired private JwtProperties properties;

  @MockitoBean private RegisterDevice registerDevice;
  @MockitoBean private ListDevices listDevices;
  @MockitoBean private RevokeDevice revokeDevice;
  @MockitoBean private AuthenticateDevice authenticateDevice;
  @MockitoBean private HardwareFactories hardware;
  @MockitoBean private IngestSensorEvents ingestSensorEvents;

  private final UserId juan = UserId.newId();
  private final HouseholdId household = HouseholdId.newId();
  private final Device device =
      Device.register(household, FridgeId.newId(), "Door", DeviceKind.ESP32_DOOR_TEMP, "ab", NOW);

  private String bearer() {
    return "Bearer "
        + new JwtAccessTokenIssuer(encoder, properties).issue(juan, Instant.now()).value();
  }

  private String path() {
    return "/api/v1/households/" + household.value() + "/devices";
  }

  @Test
  void registersAndReturnsTheKeyOnce() throws Exception {
    when(registerDevice.register(
            eq(juan), eq(household), any(), eq("Door"), eq(DeviceKind.ESP32_DOOR_TEMP)))
        .thenReturn(new RegisteredDevice(device, "hpc_dev_secret"));

    mvc.perform(
            post(path())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fridgeId\":\""
                        + device.fridge().value()
                        + "\",\"name\":\"Door\",\"kind\":\"ESP32_DOOR_TEMP\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.apiKey").value("hpc_dev_secret"))
        .andExpect(jsonPath("$.device.active").value(true))
        .andExpect(jsonPath("$.device.apiKeyHash").doesNotExist());
  }

  @Test
  void listsWithoutKeysAndRevokes() throws Exception {
    when(listDevices.list(juan, household)).thenReturn(List.of(device));

    mvc.perform(get(path()).header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Door"))
        .andExpect(jsonPath("$[0].apiKey").doesNotExist());
    mvc.perform(delete(path() + "/" + device.id().value()).header("Authorization", bearer()))
        .andExpect(status().isNoContent());
    verify(revokeDevice).revoke(juan, household, device.id());
  }

  @Test
  void devicesAuthenticateWithTheirKeyOnly() throws Exception {
    when(authenticateDevice.authenticate("hpc_dev_good")).thenReturn(device);
    when(authenticateDevice.authenticate("hpc_dev_bad")).thenThrow(new InvalidDeviceKeyException());
    when(authenticateDevice.authenticate(null)).thenThrow(new InvalidDeviceKeyException());

    mvc.perform(get("/api/v1/device/whoami").header("X-Device-Key", "hpc_dev_good"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.householdId").value(household.value().toString()))
        .andExpect(jsonPath("$.kind").value("ESP32_DOOR_TEMP"));
    mvc.perform(get("/api/v1/device/whoami").header("X-Device-Key", "hpc_dev_bad"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Invalid device key"));
    mvc.perform(get("/api/v1/device/whoami").header("Authorization", bearer()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void devicesPullTheirPendingAlertCommands() throws Exception {
    when(authenticateDevice.authenticate("hpc_dev_good")).thenReturn(device);
    QueuedAlertSignal queue = new QueuedAlertSignal();
    queue.signal(device.id(), AlertPattern.DOOR_OPEN_BEEP);
    HardwareFactory family = mock(HardwareFactory.class);
    when(family.alerts()).thenReturn(queue);
    when(hardware.forDevice(device)).thenReturn(family);

    mvc.perform(get("/api/v1/device/commands").header("X-Device-Key", "hpc_dev_good"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("DOOR_OPEN_BEEP"));
    mvc.perform(get("/api/v1/device/commands").header("X-Device-Key", "hpc_dev_good"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void deviceKeysCannotCallUserEndpoints() throws Exception {
    when(authenticateDevice.authenticate("hpc_dev_good")).thenReturn(device);

    mvc.perform(get(path()).header("X-Device-Key", "hpc_dev_good"))
        .andExpect(status().isUnauthorized());
  }
}
