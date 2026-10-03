package dev.haypacomer.application.sensor;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.domain.sensor.Finding;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class CheckFridgeAlerts {

  private final AlertDispatcher dispatcher;
  private final Clock clock;

  public CheckFridgeAlerts(
      FridgeMonitorRegistry registry,
      DeviceRepository devices,
      HardwareFactories hardware,
      Clock clock) {
    this.dispatcher = new AlertDispatcher(registry, devices, hardware);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<Finding> check() {
    Instant now = clock.instant();
    return List.copyOf(dispatcher.registry().fridges()).stream()
        .flatMap(fridge -> dispatcher.evaluate(fridge, now).stream())
        .toList();
  }
}
