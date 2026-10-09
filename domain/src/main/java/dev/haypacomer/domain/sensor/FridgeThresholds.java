package dev.haypacomer.domain.sensor;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

public record FridgeThresholds(
    Duration doorAlertAfter,
    BigDecimal maxCelsius,
    Duration coldChainGrace,
    BigDecimal minimumWeightChange) {

  public static final FridgeThresholds DEFAULT =
      new FridgeThresholds(
          Duration.ofSeconds(40),
          new BigDecimal("5.0"),
          Duration.ofMinutes(20),
          new BigDecimal("5"));

  public FridgeThresholds {
    Objects.requireNonNull(doorAlertAfter, "doorAlertAfter");
    Objects.requireNonNull(maxCelsius, "maxCelsius");
    Objects.requireNonNull(coldChainGrace, "coldChainGrace");
    Objects.requireNonNull(minimumWeightChange, "minimumWeightChange");
  }

  public FridgeThresholds withMinimumWeightChange(BigDecimal grams) {
    return new FridgeThresholds(doorAlertAfter, maxCelsius, coldChainGrace, grams);
  }
}
