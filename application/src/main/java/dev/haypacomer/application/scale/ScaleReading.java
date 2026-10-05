package dev.haypacomer.application.scale;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ScaleReading(
    DeviceId device, long rawCounts, Grams grams, boolean calibrated, Instant at) {

  public ScaleReading {
    Objects.requireNonNull(device, "device");
    Objects.requireNonNull(at, "at");
  }

  public Optional<Grams> weight() {
    return Optional.ofNullable(grams);
  }
}
