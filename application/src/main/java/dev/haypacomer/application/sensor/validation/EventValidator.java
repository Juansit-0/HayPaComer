package dev.haypacomer.application.sensor.validation;

import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Instant;
import java.util.Optional;

public abstract class EventValidator {

  private EventValidator next;

  public final EventValidator then(EventValidator successor) {
    this.next = successor;
    return successor;
  }

  public final ValidationResult validate(SensorEvent event, Instant now) {
    Optional<ValidationResult> verdict = check(event, now);
    if (verdict.isPresent()) {
      return verdict.get();
    }
    return next == null ? ValidationResult.ACCEPTED : next.validate(event, now);
  }

  protected abstract Optional<ValidationResult> check(SensorEvent event, Instant now);
}
