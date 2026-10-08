package dev.haypacomer.domain.session;

import java.time.Instant;

public record Preparing() implements SessionState {

  @Override
  public SessionPhase phase() {
    return SessionPhase.PREPARING;
  }

  @Override
  public int currentStep() {
    return 0;
  }

  @Override
  public SessionState next(int totalSteps, Instant at) {
    return totalSteps == 0 ? new Finished(0, at) : new Cooking(1);
  }

  @Override
  public SessionState abandon(Instant at) {
    return new Abandoned(0, at);
  }
}
