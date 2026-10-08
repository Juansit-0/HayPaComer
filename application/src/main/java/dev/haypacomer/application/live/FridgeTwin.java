package dev.haypacomer.application.live;

import dev.haypacomer.domain.fridge.FridgeId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record FridgeTwin(
    FridgeId fridge, Boolean doorOpen, Instant doorSince, BigDecimal celsius, Instant measuredAt) {

  public FridgeTwin {
    Objects.requireNonNull(fridge, "fridge");
  }

  public Optional<Boolean> door() {
    return Optional.ofNullable(doorOpen);
  }

  public Optional<BigDecimal> temperature() {
    return Optional.ofNullable(celsius);
  }
}
