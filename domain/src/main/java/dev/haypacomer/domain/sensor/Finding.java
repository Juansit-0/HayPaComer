package dev.haypacomer.domain.sensor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record Finding(
    FindingKind kind, String channel, Instant since, Duration duration, BigDecimal magnitude) {

  public Finding {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(channel, "channel");
    Objects.requireNonNull(since, "since");
    Objects.requireNonNull(duration, "duration");
    Objects.requireNonNull(magnitude, "magnitude");
  }
}
