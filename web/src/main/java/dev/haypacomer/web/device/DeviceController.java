package dev.haypacomer.web.device;

import dev.haypacomer.application.device.ListDevices;
import dev.haypacomer.application.device.RegisterDevice;
import dev.haypacomer.application.device.RegisteredDevice;
import dev.haypacomer.application.device.RevokeDevice;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households/{householdId}/devices")
public class DeviceController {

  private final RegisterDevice registerDevice;
  private final ListDevices listDevices;
  private final RevokeDevice revokeDevice;

  public DeviceController(
      RegisterDevice registerDevice, ListDevices listDevices, RevokeDevice revokeDevice) {
    this.registerDevice = registerDevice;
    this.listDevices = listDevices;
    this.revokeDevice = revokeDevice;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  RegisteredDeviceResponse register(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @Valid @RequestBody RegisterDeviceRequest request) {
    RegisteredDevice registered =
        registerDevice.register(
            CurrentUser.of(jwt),
            new HouseholdId(householdId),
            new FridgeId(request.fridgeId()),
            request.name(),
            request.kind());
    return new RegisteredDeviceResponse(
        DeviceResponse.from(registered.device()), registered.apiKey());
  }

  @GetMapping
  List<DeviceResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID householdId) {
    return listDevices.list(CurrentUser.of(jwt), new HouseholdId(householdId)).stream()
        .map(DeviceResponse::from)
        .toList();
  }

  @DeleteMapping("/{deviceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void revoke(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID householdId,
      @PathVariable UUID deviceId) {
    revokeDevice.revoke(CurrentUser.of(jwt), new HouseholdId(householdId), new DeviceId(deviceId));
  }

  record RegisterDeviceRequest(
      @NotNull UUID fridgeId, @NotBlank @Size(max = 60) String name, @NotNull DeviceKind kind) {}

  record DeviceResponse(
      UUID id,
      UUID fridgeId,
      String name,
      DeviceKind kind,
      boolean active,
      Instant createdAt,
      Instant lastSeenAt) {

    static DeviceResponse from(Device device) {
      return new DeviceResponse(
          device.id().value(),
          device.fridge().value(),
          device.name(),
          device.kind(),
          device.isActive(),
          device.createdAt(),
          device.lastSeenAt());
    }
  }

  record RegisteredDeviceResponse(DeviceResponse device, String apiKey) {

    @Override
    public String toString() {
      return "RegisteredDeviceResponse[device=" + device + "]";
    }
  }
}
