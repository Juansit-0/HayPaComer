package dev.haypacomer.application.sensor.validation;

import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class DuplicateValidator extends EventValidator {

  private final SensorEventLog log;

  public DuplicateValidator(SensorEventLog log) {
    this.log = Objects.requireNonNull(log, "log");
  }

  @Override
  protected Optional<ValidationResult> check(SensorEvent event, Instant now) {
    return log.contains(event.id()) ? Optional.of(ValidationResult.duplicate()) : Optional.empty();
  }
}
