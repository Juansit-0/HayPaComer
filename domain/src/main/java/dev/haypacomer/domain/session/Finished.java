package dev.haypacomer.domain.session;

import java.time.Instant;
import java.util.Objects;

public record Finished(int currentStep, Instant at) implements SessionState {

  public Finished {
    Objects.requireNonNull(at, "at");
  }

  @Override
  public SessionPhase phase() {
    return SessionPhase.FINISHED;
  }
}
