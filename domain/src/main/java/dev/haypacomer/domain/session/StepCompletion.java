package dev.haypacomer.domain.session;

import java.time.Instant;
import java.util.Objects;

public record StepCompletion(int position, Instant at) {

  public StepCompletion {
    Objects.requireNonNull(at, "at");
    if (position < 1) {
      throw new IllegalArgumentException("Step position starts at 1: " + position);
    }
  }
}
