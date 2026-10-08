package dev.haypacomer.application.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record AiAuditEntry(
    String provider, String operation, Duration latency, AiOutcome outcome, Instant at) {

  public AiAuditEntry {
    Objects.requireNonNull(provider, "provider");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(latency, "latency");
    Objects.requireNonNull(outcome, "outcome");
    Objects.requireNonNull(at, "at");
    if (latency.isNegative()) {
      throw new IllegalArgumentException("Latency cannot be negative");
    }
  }
}
