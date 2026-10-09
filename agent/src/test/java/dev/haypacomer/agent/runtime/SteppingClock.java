package dev.haypacomer.agent.runtime;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

final class SteppingClock extends Clock {

  private Instant now;
  private final Duration tick;

  SteppingClock(Instant start, Duration tick) {
    this.now = start;
    this.tick = tick;
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return this;
  }

  @Override
  public Instant instant() {
    Instant current = now;
    now = now.plus(tick);
    return current;
  }
}
