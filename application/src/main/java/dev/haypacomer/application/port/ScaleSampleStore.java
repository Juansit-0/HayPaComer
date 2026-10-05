package dev.haypacomer.application.port;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.RawSample;
import java.time.Instant;
import java.util.Optional;

public interface ScaleSampleStore {

  void record(DeviceId device, RawSample sample);

  Optional<RawSample> latest(DeviceId device);

  boolean stable(DeviceId device, Grams grams, Instant at);
}
