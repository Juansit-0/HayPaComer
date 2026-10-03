package dev.haypacomer.application.sensor.validation;

import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.WeightReading;
import java.time.Instant;
import java.util.Optional;

public final class StabilityValidator extends EventValidator {

  @Override
  protected Optional<ValidationResult> check(SensorEvent event, Instant now) {
    if (event instanceof WeightReading reading && !reading.stable()) {
      return Optional.of(ValidationResult.dropped("weight is not stable yet"));
    }
    return Optional.empty();
  }
}
