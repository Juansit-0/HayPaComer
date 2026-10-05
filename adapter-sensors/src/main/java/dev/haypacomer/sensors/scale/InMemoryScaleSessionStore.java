package dev.haypacomer.sensors.scale;

import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.ScaleMode;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryScaleSessionStore implements ScaleSessionStore {

  private final Map<DeviceId, WeighingTarget> cooking = new ConcurrentHashMap<>();

  @Override
  public ScaleMode mode(DeviceId scale) {
    return cooking.containsKey(scale) ? ScaleMode.COOKING : ScaleMode.FRIDGE;
  }

  @Override
  public Optional<WeighingTarget> target(DeviceId scale) {
    return Optional.ofNullable(cooking.get(scale));
  }

  @Override
  public void cook(DeviceId scale, WeighingTarget target) {
    cooking.put(scale, target);
  }

  @Override
  public void fridge(DeviceId scale) {
    cooking.remove(scale);
  }
}
