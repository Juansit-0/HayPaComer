package dev.haypacomer.sensors.scale;

import dev.haypacomer.application.port.ScaleCalibrationRepository;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.scale.ScaleCalibration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCalibrations implements ScaleCalibrationRepository {

  private final Map<DeviceId, ScaleCalibration> calibrations = new HashMap<>();

  @Override
  public Optional<ScaleCalibration> find(DeviceId device) {
    return Optional.ofNullable(calibrations.get(device));
  }

  @Override
  public void save(DeviceId device, ScaleCalibration calibration) {
    calibrations.put(device, calibration);
  }
}
