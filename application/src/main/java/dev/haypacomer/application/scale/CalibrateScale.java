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
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.ScaleCalibration;
import java.time.Clock;
import java.util.Objects;

public final class CalibrateScale {

  private final ScaleAccess access;
  private final ScaleCalibrationRepository calibrations;

  public CalibrateScale(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleCalibrationRepository calibrations,
      Clock clock) {
    this.access = new ScaleAccess(households, devices, samples, clock);
    this.calibrations = Objects.requireNonNull(calibrations, "calibrations");
  }

  public ScaleCalibration calibrate(
      UserId actor, HouseholdId household, DeviceId deviceId, Grams knownWeight) {
    Device device = access.scale(actor, household, deviceId, Permission.MANAGE_DEVICES);
    ScaleCalibration tared =
        calibrations
            .find(device.id())
            .orElseThrow(
                () -> new IllegalStateException("Tare the empty scale before calibrating"));
    ScaleCalibration calibrated = tared.calibrate(access.freshSample(device.id()), knownWeight);
    calibrations.save(device.id(), calibrated);
    return calibrated;
  }
}
