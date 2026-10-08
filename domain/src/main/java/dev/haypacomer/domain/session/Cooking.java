package dev.haypacomer.domain.session;

import java.time.Instant;

public record Cooking(int currentStep) implements SessionState {

  public Cooking {
    if (currentStep < 1) {
      throw new IllegalArgumentException("Cooking starts at step 1: " + currentStep);
    }
  }

  @Override
  public SessionPhase phase() {
    return SessionPhase.COOKING;
  }

  @Override
  public SessionState next(int totalSteps, Instant at) {
    return currentStep >= totalSteps ? new Finished(currentStep, at) : new Cooking(currentStep + 1);
  }

  @Override
  public SessionState pause(Instant at) {
    return new Paused(currentStep, at);
  }

  @Override
  public SessionState abandon(Instant at) {
    return new Abandoned(currentStep, at);
  }
}
