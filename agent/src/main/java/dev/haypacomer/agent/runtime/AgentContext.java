package dev.haypacomer.agent.runtime;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record AgentContext(
    AgentTask task, Set<String> tools, List<Exchange> history, int stepsLeft) {

  public AgentContext {
    Objects.requireNonNull(task, "task");
    tools = Set.copyOf(tools);
    history = List.copyOf(history);
  }

  public boolean called(String tool) {
    return history.stream().anyMatch(exchange -> exchange.call().tool().equals(tool));
  }
}
