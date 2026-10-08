package dev.haypacomer.application.session;

import java.time.Duration;
import java.util.Objects;

public record TimerStatus(
    int step, Duration duration, Duration remaining, boolean paused, boolean done) {

  public TimerStatus {
    Objects.requireNonNull(duration, "duration");
    Objects.requireNonNull(remaining, "remaining");
  }
}
