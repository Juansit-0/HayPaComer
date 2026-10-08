package dev.haypacomer.domain.session;

import java.time.Instant;
import java.util.Objects;

public record Abandoned(int currentStep, Instant at) implements SessionState {

  public Abandoned {
    Objects.requireNonNull(at, "at");
  }

  @Override
  public SessionPhase phase() {
    return SessionPhase.ABANDONED;
  }
}
