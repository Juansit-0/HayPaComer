package dev.haypacomer.application.scale;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.ScaleCalibrationRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.scale.ScaleCalibration;
import java.time.Clock;
import java.util.Objects;

public final class TareScale {

  private final ScaleAccess access;
  private final ScaleCalibrationRepository calibrations;

  public TareScale(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleCalibrationRepository calibrations,
      Clock clock) {
    this.access = new ScaleAccess(households, devices, samples, clock);
    this.calibrations = Objects.requireNonNull(calibrations, "calibrations");
  }

  public ScaleCalibration tare(UserId actor, HouseholdId household, DeviceId deviceId) {
    Device device = access.scale(actor, household, deviceId, Permission.EDIT_INVENTORY);
    RawSample sample = access.freshSample(device.id());
    ScaleCalibration calibration =
        calibrations
            .find(device.id())
            .map(existing -> existing.tare(sample))
            .orElseGet(() -> ScaleCalibration.taredAt(sample));
    calibrations.save(device.id(), calibration);
    return calibration;
  }
}
