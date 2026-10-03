package dev.haypacomer.web.device;

import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceSelfController {

  private final HardwareFactories hardware;

  public DeviceSelfController(HardwareFactories hardware) {
    this.hardware = hardware;
  }

  @GetMapping("/commands")
  List<AlertPattern> commands(@AuthenticationPrincipal Device device) {
    return hardware.forDevice(device).alerts().drain(device.id());
  }

  @GetMapping("/whoami")
  Identity whoami(@AuthenticationPrincipal Device device) {
    return new Identity(
        device.id().value(),
        device.household().value(),
        device.fridge().value(),
        device.name(),
        device.kind());
  }

  record Identity(UUID id, UUID householdId, UUID fridgeId, String name, DeviceKind kind) {}
}
