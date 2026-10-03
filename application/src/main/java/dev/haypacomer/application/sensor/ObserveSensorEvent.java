package dev.haypacomer.application.sensor;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class ObserveSensorEvent {

  private final AlertDispatcher dispatcher;
  private final Clock clock;

  public ObserveSensorEvent(
      FridgeMonitorRegistry registry,
      DeviceRepository devices,
      HardwareFactories hardware,
      Clock clock) {
    this.dispatcher = new AlertDispatcher(registry, devices, hardware);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<Finding> observe(Device device, SensorEvent event) {
    if (!event.device().equals(device.id()) || !event.fridge().equals(device.fridge())) {
      throw new IllegalArgumentException("Event does not belong to this device");
    }
    FridgeMonitor monitor = dispatcher.registry().monitor(device.fridge());
    synchronized (monitor) {
      monitor.record(event);
    }
    dispatcher.registry().rememberDevice(device.fridge(), device.id());
    Instant now = clock.instant();
    Instant evaluatedAt = event.occurredAt().isAfter(now) ? event.occurredAt() : now;
    return dispatcher.evaluate(device.fridge(), evaluatedAt);
  }
}
