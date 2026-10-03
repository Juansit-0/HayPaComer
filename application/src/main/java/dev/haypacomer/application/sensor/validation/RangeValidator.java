package dev.haypacomer.application.sensor.validation;

import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.TemperatureReading;
import dev.haypacomer.domain.sensor.WeightReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public final class RangeValidator extends EventValidator {

  private static final BigDecimal MIN_CELSIUS = new BigDecimal("-30");
  private static final BigDecimal MAX_CELSIUS = new BigDecimal("60");
  private static final BigDecimal MAX_GRAMS = new BigDecimal("20000");

  @Override
  protected Optional<ValidationResult> check(SensorEvent event, Instant now) {
    if (event instanceof TemperatureReading reading
        && (reading.celsius().compareTo(MIN_CELSIUS) < 0
            || reading.celsius().compareTo(MAX_CELSIUS) > 0)) {
      return Optional.of(ValidationResult.rejected("tempC outside [-30, 60]"));
    }
    if (event instanceof WeightReading reading
        && reading.grams().value().compareTo(MAX_GRAMS) > 0) {
      return Optional.of(ValidationResult.rejected("grams outside [0, 20000]"));
    }
    return Optional.empty();
  }
}
