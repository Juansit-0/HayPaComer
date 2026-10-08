package dev.haypacomer.application.sensor;

import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.application.notification.NotifyHousehold;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class AlertDispatcher {

  private final FridgeMonitorRegistry registry;
  private final DeviceRepository devices;
  private final HardwareFactories hardware;
  private final NotifyHousehold notifications;

  AlertDispatcher(
      FridgeMonitorRegistry registry,
      DeviceRepository devices,
      HardwareFactories hardware,
      NotifyHousehold notifications) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.devices = Objects.requireNonNull(devices, "devices");
    this.hardware = Objects.requireNonNull(hardware, "hardware");
    this.notifications = Objects.requireNonNull(notifications, "notifications");
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
                    finding -> {
                      hardware.forDevice(device).alerts().signal(device.id(), pattern(finding));
                      notification(device.household(), finding, now)
                          .ifPresent(notifications::publish);
                    }));
    return fresh;
  }

  private static Optional<Notification> notification(
      HouseholdId household, Finding finding, Instant now) {
    return switch (finding.kind()) {
      case DOOR_LEFT_OPEN ->
          Optional.of(
              Notification.of(
                  household,
                  NotificationType.DOOR_LEFT_OPEN,
                  "Fridge door left open",
                  "The fridge door has been open for "
                      + finding.duration().toSeconds()
                      + " s. Close it to keep the food cold.",
                  now));
      case COLD_CHAIN_BREACH ->
          Optional.of(
              Notification.of(
                  household,
                  NotificationType.COLD_CHAIN_BREACH,
                  "Fridge too warm",
                  "The fridge stayed at "
                      + finding.magnitude().stripTrailingZeros().toPlainString()
                      + " C for "
                      + finding.duration().toMinutes()
                      + " min. Review the perishable food before eating it.",
                  now));
      case STOCK_DECREASE, STOCK_INCREASE -> Optional.empty();
    };
  }

  private static AlertPattern pattern(Finding finding) {
    return switch (finding.kind()) {
      case DOOR_LEFT_OPEN -> AlertPattern.DOOR_OPEN_BEEP;
      case COLD_CHAIN_BREACH -> AlertPattern.COLD_CHAIN_ALARM;
      case STOCK_DECREASE, STOCK_INCREASE -> AlertPattern.WEIGHT_CONFIRMED_BLINK;
    };
  }
}
