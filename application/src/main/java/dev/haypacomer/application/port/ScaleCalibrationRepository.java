package dev.haypacomer.application.port;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.scale.ScaleCalibration;
import java.util.Optional;

public interface ScaleCalibrationRepository {

  Optional<ScaleCalibration> find(DeviceId device);

  void save(DeviceId device, ScaleCalibration calibration);
}
