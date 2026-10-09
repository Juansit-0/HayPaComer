package dev.haypacomer.agent.runtime;

import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.PendingConfirmation;
import java.util.Objects;
import java.util.Optional;

public record AgentResult(AgentRun run, String answer, PendingConfirmation confirmation) {

  public AgentResult {
    Objects.requireNonNull(run, "run");
  }

  public Optional<String> answerText() {
    return Optional.ofNullable(answer);
  }

  public Optional<PendingConfirmation> pending() {
    return Optional.ofNullable(confirmation);
  }
}
