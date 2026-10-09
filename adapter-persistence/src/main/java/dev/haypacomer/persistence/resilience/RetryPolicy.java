package dev.haypacomer.persistence.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

public final class RetryPolicy {

  public interface Sleeper {
    void sleep(Duration duration) throws InterruptedException;
  }

  public static final Sleeper THREAD_SLEEP = duration -> Thread.sleep(duration.toMillis());

  private final int attempts;
  private final Duration firstDelay;
  private final int multiplier;
  private final Sleeper sleeper;

  public RetryPolicy(int attempts, Duration firstDelay, int multiplier, Sleeper sleeper) {
    if (attempts < 1 || multiplier < 1 || firstDelay.isNegative()) {
      throw new IllegalArgumentException("A retry policy needs at least one attempt");
    }
    this.attempts = attempts;
    this.firstDelay = firstDelay;
    this.multiplier = multiplier;
    this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
  }

  public <T> T run(Supplier<T> call) {
    Duration delay = firstDelay;
    RuntimeException last = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        return call.get();
      } catch (IllegalArgumentException | IllegalStateException rule) {
        throw rule;
      } catch (RuntimeException failure) {
        last = failure;
        if (attempt < attempts) {
          pause(delay);
          delay = delay.multipliedBy(multiplier);
        }
      }
    }
    throw last;
  }

  private void pause(Duration delay) {
    try {
      sleeper.sleep(delay);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting to retry", interrupted);
    }
  }
}
