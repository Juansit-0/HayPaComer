package dev.haypacomer.agent.supervisor;

import dev.haypacomer.agent.runtime.AgentResult;
import dev.haypacomer.application.agent.PendingConfirmation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SupervisorAnswer(List<AgentResult> parts, String answer) {

  public SupervisorAnswer {
    parts = List.copyOf(parts);
    Objects.requireNonNull(answer, "answer");
  }

  public Optional<PendingConfirmation> pending() {
    return parts.stream().flatMap(part -> part.pending().stream()).findFirst();
  }
}
