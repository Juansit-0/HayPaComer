package dev.haypacomer.domain.session;

import java.time.Instant;
import java.util.Objects;

public record Paused(int currentStep, Instant since) implements SessionState {

  public Paused {
    Objects.requireNonNull(since, "since");
    if (currentStep < 1) {
      throw new IllegalArgumentException("Only a cooking step can be paused: " + currentStep);
    }
  }

  @Override
  public SessionPhase phase() {
    return SessionPhase.PAUSED;
  }

  @Override
  public SessionState resume(Instant at) {
    return new Cooking(currentStep);
  }

  @Override
  public SessionState abandon(Instant at) {
    return new Abandoned(currentStep, at);
  }
}
