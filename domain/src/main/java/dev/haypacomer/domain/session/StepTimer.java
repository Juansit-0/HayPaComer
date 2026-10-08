package dev.haypacomer.domain.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record StepTimer(
    CookingSessionId session,
    int step,
    Duration duration,
    Instant startedAt,
    Duration elapsedBeforePause,
    Instant pausedAt,
    boolean signaled) {

  public StepTimer {
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(duration, "duration");
    Objects.requireNonNull(startedAt, "startedAt");
    Objects.requireNonNull(elapsedBeforePause, "elapsedBeforePause");
    if (step < 1) {
      throw new IllegalArgumentException("Timers belong to a step: " + step);
    }
    if (duration.isNegative() || duration.isZero()) {
      throw new IllegalArgumentException("Timer duration must be positive: " + duration);
    }
  }

  public static StepTimer start(CookingSessionId session, int step, Duration duration, Instant at) {
    return new StepTimer(session, step, duration, at, Duration.ZERO, null, false);
  }

  public boolean paused() {
    return pausedAt != null;
  }

  public Duration elapsed(Instant now) {
    if (paused() || now.isBefore(startedAt)) {
      return elapsedBeforePause;
    }
    return elapsedBeforePause.plus(Duration.between(startedAt, now));
  }

  public Duration remaining(Instant now) {
    Duration left = duration.minus(elapsed(now));
    return left.isNegative() ? Duration.ZERO : left;
  }

  public boolean due(Instant now) {
    return !paused() && remaining(now).isZero();
  }

  public StepTimer pause(Instant at) {
    if (paused()) {
      return this;
    }
    return new StepTimer(session, step, duration, startedAt, elapsed(at), at, signaled);
  }

  public StepTimer resume(Instant at) {
    if (!paused()) {
      return this;
    }
    return new StepTimer(session, step, duration, at, elapsedBeforePause, null, signaled);
  }

  public StepTimer markSignaled() {
    return new StepTimer(session, step, duration, startedAt, elapsedBeforePause, pausedAt, true);
  }
}
