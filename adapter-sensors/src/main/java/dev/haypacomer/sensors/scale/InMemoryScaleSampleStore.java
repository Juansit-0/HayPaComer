package dev.haypacomer.sensors.scale;

import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.scale.StabilityDetector;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryScaleSampleStore implements ScaleSampleStore {

  private final Map<DeviceId, RawSample> latest = new ConcurrentHashMap<>();
  private final Map<DeviceId, StabilityDetector> detectors = new ConcurrentHashMap<>();

  @Override
  public void record(DeviceId device, RawSample sample) {
    latest.merge(device, sample, (old, fresh) -> fresh.at().isBefore(old.at()) ? old : fresh);
  }

  @Override
  public Optional<RawSample> latest(DeviceId device) {
    return Optional.ofNullable(latest.get(device));
  }

  @Override
  public boolean stable(DeviceId device, Grams grams, Instant at) {
    return detectors.computeIfAbsent(device, id -> StabilityDetector.standard()).offer(grams, at);
  }
}
