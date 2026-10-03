package dev.haypacomer.application.sensor;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.port.AlertSignal;
import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.application.port.SensorEventDecoder;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HardwareFactoriesTest {

  private static HardwareFactory factory(Set<DeviceKind> kinds) {
    return new HardwareFactory() {
      @Override
      public boolean supports(DeviceKind kind) {
        return kinds.contains(kind);
      }

      @Override
      public SensorEventDecoder decoder() {
        return (device, payload) -> List.of();
      }

      @Override
      public AlertSignal alerts() {
        return null;
      }
    };
  }

  private static Device device(DeviceKind kind) {
    return Device.register(
        HouseholdId.newId(), FridgeId.newId(), "Device", kind, "ab", Instant.EPOCH);
  }

  @Test
  void picksTheFamilyForEachDeviceKind() {
    HardwareFactory real = factory(Set.of(DeviceKind.ESP32_DOOR_TEMP, DeviceKind.ESP32_SCALE));
    HardwareFactory simulated = factory(Set.of(DeviceKind.SIMULATOR));
    HardwareFactories factories = new HardwareFactories(List.of(real, simulated));

    assertSame(real, factories.forDevice(device(DeviceKind.ESP32_SCALE)));
    assertSame(simulated, factories.forDevice(device(DeviceKind.SIMULATOR)));
  }

  @Test
  void requiresExactlyOneFamilyPerKind() {
    HardwareFactory real = factory(Set.of(DeviceKind.ESP32_DOOR_TEMP, DeviceKind.ESP32_SCALE));

    assertThrows(IllegalStateException.class, () -> new HardwareFactories(List.of(real)));
    assertThrows(
        IllegalStateException.class,
        () -> new HardwareFactories(List.of(real, factory(Set.of(DeviceKind.values())))));
  }
}
