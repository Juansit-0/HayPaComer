package dev.haypacomer.application.ai;

import java.time.Duration;
import java.util.Objects;

public record CircuitPolicy(int failureThreshold, Duration openFor) {

  public static final CircuitPolicy DEFAULT = new CircuitPolicy(3, Duration.ofMinutes(1));

  public CircuitPolicy {
    Objects.requireNonNull(openFor, "openFor");
    if (failureThreshold < 1) {
      throw new IllegalArgumentException("Failure threshold must be at least 1");
    }
    if (openFor.isNegative() || openFor.isZero()) {
      throw new IllegalArgumentException("Open time must be positive");
    }
  }
}
