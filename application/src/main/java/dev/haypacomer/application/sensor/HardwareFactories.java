package dev.haypacomer.application.sensor;

import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import java.util.List;

public final class HardwareFactories {

  private final List<HardwareFactory> factories;

  public HardwareFactories(List<HardwareFactory> factories) {
    this.factories = List.copyOf(factories);
    for (DeviceKind kind : DeviceKind.values()) {
      long matches = this.factories.stream().filter(factory -> factory.supports(kind)).count();
      if (matches != 1) {
        throw new IllegalStateException(
            "Exactly one hardware factory must support " + kind + ", found " + matches);
      }
    }
  }

  public HardwareFactory forDevice(Device device) {
    return factories.stream()
        .filter(factory -> factory.supports(device.kind()))
        .findFirst()
        .orElseThrow();
  }
}
