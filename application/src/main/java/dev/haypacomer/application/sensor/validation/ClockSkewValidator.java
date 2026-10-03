package dev.haypacomer.application.sensor.validation;

import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ClockSkewValidator extends EventValidator {

  private final Duration maxFuture;
  private final Duration maxPast;

  public ClockSkewValidator(Duration maxFuture, Duration maxPast) {
    this.maxFuture = Objects.requireNonNull(maxFuture, "maxFuture");
    this.maxPast = Objects.requireNonNull(maxPast, "maxPast");
  }

  @Override
  protected Optional<ValidationResult> check(SensorEvent event, Instant now) {
    if (event.occurredAt().isAfter(now.plus(maxFuture))) {
      return Optional.of(ValidationResult.rejected("timestamp is in the future"));
    }
    if (event.occurredAt().isBefore(now.minus(maxPast))) {
      return Optional.of(ValidationResult.rejected("timestamp is too old"));
    }
    return Optional.empty();
  }
}
