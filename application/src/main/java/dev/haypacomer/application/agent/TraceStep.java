package dev.haypacomer.application.agent;

import java.time.Instant;
import java.util.Objects;

public record TraceStep(TraceKind kind, String detail, Instant at) {

  public TraceStep {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(detail, "detail");
    Objects.requireNonNull(at, "at");
  }
}
