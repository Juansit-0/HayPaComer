package dev.haypacomer.domain.session;

import java.time.Instant;

public sealed interface SessionState permits Preparing, Cooking, Paused, Finished, Abandoned {

  SessionPhase phase();

  int currentStep();

  default SessionState next(int totalSteps, Instant at) {
    throw new IllegalSessionTransitionException(phase(), "advance");
  }

  default SessionState pause(Instant at) {
    throw new IllegalSessionTransitionException(phase(), "pause");
  }

  default SessionState resume(Instant at) {
    throw new IllegalSessionTransitionException(phase(), "resume");
  }

  default SessionState abandon(Instant at) {
    throw new IllegalSessionTransitionException(phase(), "abandon");
  }
}
