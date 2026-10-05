package dev.haypacomer.application.port;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.ScaleMode;
import java.util.Optional;

public interface ScaleSessionStore {

  ScaleMode mode(DeviceId scale);

  Optional<WeighingTarget> target(DeviceId scale);

  void cook(DeviceId scale, WeighingTarget target);

  void fridge(DeviceId scale);
}
