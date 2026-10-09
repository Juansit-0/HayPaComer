package dev.haypacomer.application.resilience;

import java.time.Instant;
import java.util.Objects;

public record DegradedComponent(String component, String reason, Instant since) {

  public DegradedComponent {
    Objects.requireNonNull(component, "component");
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(since, "since");
  }
}
