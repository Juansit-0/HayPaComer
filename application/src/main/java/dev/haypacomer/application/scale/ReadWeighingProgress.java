package dev.haypacomer.application.scale;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.ScaleCalibrationRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.scale.WeighingTarget;
import java.time.Clock;
import java.util.Objects;

public final class ReadWeighingProgress {

  private final ScaleAccess access;
  private final ScaleCalibrationRepository calibrations;
  private final ScaleSessionStore sessions;

  public ReadWeighingProgress(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleCalibrationRepository calibrations,
      ScaleSessionStore sessions,
      Clock clock) {
    this.access = new ScaleAccess(households, devices, samples, clock);
    this.calibrations = Objects.requireNonNull(calibrations, "calibrations");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
  }

  public WeighingProgress read(UserId actor, HouseholdId household, DeviceId deviceId) {
    Device scale = access.scale(actor, household, deviceId, Permission.VIEW_HOUSEHOLD);
    WeighingTarget target =
        sessions
            .target(scale.id())
            .orElseThrow(() -> new IllegalStateException("The scale is not in cooking mode"));
    long counts = access.freshSample(scale.id()).counts();
    Grams measured =
        calibrations
            .find(scale.id())
            .flatMap(calibration -> calibration.toGrams(counts))
            .orElseThrow(() -> new IllegalStateException("Calibrate the scale first"));
    return target.evaluate(measured);
  }
}
