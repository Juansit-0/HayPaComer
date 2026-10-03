package dev.haypacomer.sensors.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FindingKind;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.sensors.monitor.InMemoryFridgeMonitorRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryFridgeMonitorRegistryTest {

  @Test
  void keepsOneMonitorPerFridgeAndReportsEachEpisodeOnce() {
    InMemoryFridgeMonitorRegistry registry =
        new InMemoryFridgeMonitorRegistry(FridgeThresholds.DEFAULT);
    FridgeId fridge = FridgeId.newId();
    DeviceId device = DeviceId.newId();
    Finding open =
        new Finding(
            FindingKind.DOOR_LEFT_OPEN,
            "door",
            Instant.EPOCH,
            Duration.ofSeconds(41),
            BigDecimal.ONE);

    assertSame(registry.monitor(fridge), registry.monitor(fridge));
    assertTrue(registry.lastDevice(fridge).isEmpty());
    registry.rememberDevice(fridge, device);

    assertEquals(device, registry.lastDevice(fridge).orElseThrow());
    assertEquals(List.of(fridge), List.copyOf(registry.fridges()));
    assertTrue(registry.firstReport(fridge, open));
    assertFalse(registry.firstReport(fridge, open));
  }
}
