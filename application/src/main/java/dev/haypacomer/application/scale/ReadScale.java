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
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.scale.ScaleCalibration;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class ReadScale {

  private final ScaleAccess access;
  private final ScaleCalibrationRepository calibrations;

  public ReadScale(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleCalibrationRepository calibrations,
      Clock clock) {
    this.access = new ScaleAccess(households, devices, samples, clock);
    this.calibrations = Objects.requireNonNull(calibrations, "calibrations");
  }

  public ScaleReading read(UserId actor, HouseholdId household, DeviceId deviceId) {
    Device device = access.scale(actor, household, deviceId, Permission.VIEW_HOUSEHOLD);
    RawSample sample = access.anySample(device.id());
    Optional<ScaleCalibration> calibration = calibrations.find(device.id());
    Grams grams = calibration.flatMap(value -> value.toGrams(sample.counts())).orElse(null);
    return new ScaleReading(
        device.id(),
        sample.counts(),
        grams,
        calibration.map(ScaleCalibration::isCalibrated).orElse(false),
        sample.at());
  }
}
