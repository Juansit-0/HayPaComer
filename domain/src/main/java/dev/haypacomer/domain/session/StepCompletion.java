package dev.haypacomer.domain.session;

import dev.haypacomer.domain.quantity.Grams;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record StepCompletion(int position, Instant at, Grams measured) {

  public StepCompletion {
    Objects.requireNonNull(at, "at");
    if (position < 1) {
      throw new IllegalArgumentException("Step position starts at 1: " + position);
    }
  }

  public StepCompletion(int position, Instant at) {
    this(position, at, null);
  }

  public Optional<Grams> measuredGrams() {
    return Optional.ofNullable(measured);
  }
}
