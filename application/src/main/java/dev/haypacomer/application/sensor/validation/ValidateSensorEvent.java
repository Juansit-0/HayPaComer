package dev.haypacomer.application.sensor.validation;

import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public final class ValidateSensorEvent {

  private final EventValidator chain;
  private final Clock clock;

  public ValidateSensorEvent(SensorEventLog log, Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
    EventValidator first = new RangeValidator();
    first
        .then(new ClockSkewValidator(Duration.ofMinutes(2), Duration.ofDays(7)))
        .then(new StabilityValidator())
        .then(new DuplicateValidator(log))
        .then(new StaleReadingValidator(log));
    this.chain = first;
  }

  public ValidationResult validate(SensorEvent event) {
    return chain.validate(event, clock.instant());
  }
}
