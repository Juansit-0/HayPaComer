package dev.haypacomer.application.scale;

import dev.haypacomer.application.device.DeviceNotFoundException;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.scale.RawSample;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

final class ScaleAccess {

  private static final Duration FRESHNESS = Duration.ofSeconds(10);

  private final GetHousehold households;
  private final DeviceRepository devices;
  private final ScaleSampleStore samples;
  private final Clock clock;

  ScaleAccess(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.devices = Objects.requireNonNull(devices, "devices");
    this.samples = Objects.requireNonNull(samples, "samples");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  Device scale(UserId actor, HouseholdId household, DeviceId id, Permission permission) {
    households.get(actor, household).requirePermission(actor, permission);
    return devices
        .findById(id)
        .filter(device -> device.household().equals(household))
        .filter(device -> device.kind() != DeviceKind.ESP32_DOOR_TEMP)
        .filter(Device::isActive)
        .orElseThrow(DeviceNotFoundException::new);
  }

  RawSample freshSample(DeviceId device) {
    return samples
        .latest(device)
        .filter(sample -> !sample.at().isBefore(clock.instant().minus(FRESHNESS)))
        .orElseThrow(NoRecentSampleException::new);
  }

  RawSample anySample(DeviceId device) {
    return samples.latest(device).orElseThrow(NoRecentSampleException::new);
  }
}
