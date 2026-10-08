package dev.haypacomer.application.ai;

import java.time.Instant;
import java.util.Objects;

public record CircuitState(CircuitPhase phase, int failures, Instant openedAt) {

  public static final CircuitState CLOSED = new CircuitState(CircuitPhase.CLOSED, 0, null);

  public CircuitState {
    Objects.requireNonNull(phase, "phase");
    if (failures < 0) {
      throw new IllegalArgumentException("Failures cannot be negative");
    }
    if (phase != CircuitPhase.CLOSED && openedAt == null) {
      throw new IllegalArgumentException("An open circuit needs the time it opened");
    }
  }

  public boolean allows(Instant now, CircuitPolicy policy) {
    return phase != CircuitPhase.OPEN || !now.isBefore(openedAt.plus(policy.openFor()));
  }

  public CircuitState attempt(Instant now, CircuitPolicy policy) {
    if (phase == CircuitPhase.OPEN && allows(now, policy)) {
      return new CircuitState(CircuitPhase.HALF_OPEN, failures, openedAt);
    }
    return this;
  }

  public CircuitState success() {
    return CLOSED;
  }

  public CircuitState failure(Instant now, CircuitPolicy policy) {
    int total = failures + 1;
    if (phase == CircuitPhase.HALF_OPEN || total >= policy.failureThreshold()) {
      return new CircuitState(CircuitPhase.OPEN, total, now);
    }
    return new CircuitState(CircuitPhase.CLOSED, total, null);
  }
}
