package dev.haypacomer.application.sensor.validation;

import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class StaleReadingValidator extends EventValidator {

  private final SensorEventLog log;

  public StaleReadingValidator(SensorEventLog log) {
    this.log = Objects.requireNonNull(log, "log");
  }

  @Override
  protected Optional<ValidationResult> check(SensorEvent event, Instant now) {
    return log.lastAccepted(event.device(), SensorEventTypes.of(event))
        .filter(last -> event.occurredAt().isBefore(last))
        .map(last -> ValidationResult.dropped("older than the last accepted reading"));
  }
}
