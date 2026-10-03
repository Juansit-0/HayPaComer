package dev.haypacomer.domain.sensor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Measurement(Instant at, BigDecimal value, boolean reliable) {

  public Measurement {
    Objects.requireNonNull(at, "at");
    Objects.requireNonNull(value, "value");
  }
}
