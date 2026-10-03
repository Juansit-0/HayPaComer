package dev.haypacomer.application.sensor;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

final class AlertDispatcher {

  private final FridgeMonitorRegistry registry;
  private final DeviceRepository devices;
  private final HardwareFactories hardware;

  AlertDispatcher(
      FridgeMonitorRegistry registry, DeviceRepository devices, HardwareFactories hardware) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.devices = Objects.requireNonNull(devices, "devices");
    this.hardware = Objects.requireNonNull(hardware, "hardware");
  }

  FridgeMonitorRegistry registry() {
    return registry;
  }

  List<Finding> evaluate(FridgeId fridge, Instant now) {
    FridgeMonitor monitor = registry.monitor(fridge);
    List<Finding> findings;
    synchronized (monitor) {
      findings = monitor.evaluate(now);
    }
    List<Finding> fresh =
        findings.stream().filter(finding -> registry.firstReport(fridge, finding)).toList();
    registry
        .lastDevice(fridge)
        .flatMap(devices::findById)
        .ifPresent(
            device ->
                fresh.forEach(
                    finding ->
                        hardware.forDevice(device).alerts().signal(device.id(), pattern(finding))));
    return fresh;
  }

  private static AlertPattern pattern(Finding finding) {
    return switch (finding.kind()) {
      case DOOR_LEFT_OPEN -> AlertPattern.DOOR_OPEN_BEEP;
      case COLD_CHAIN_BREACH -> AlertPattern.COLD_CHAIN_ALARM;
      case STOCK_DECREASE, STOCK_INCREASE -> AlertPattern.WEIGHT_CONFIRMED_BLINK;
    };
  }
}
