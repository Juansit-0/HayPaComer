package dev.haypacomer.application.agent;

import java.util.List;
import java.util.Objects;

public record RunTrace(AgentRun run, List<TraceStep> steps) {

  public RunTrace {
    Objects.requireNonNull(run, "run");
    steps = List.copyOf(steps);
  }
}
