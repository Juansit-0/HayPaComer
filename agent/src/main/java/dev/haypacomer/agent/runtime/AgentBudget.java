package dev.haypacomer.agent.runtime;

import java.time.Duration;
import java.util.Objects;

public record AgentBudget(int maxSteps, Duration timeout) {

  public static final AgentBudget DEFAULT = new AgentBudget(8, Duration.ofSeconds(30));

  public AgentBudget {
    Objects.requireNonNull(timeout, "timeout");
    if (maxSteps < 1 || maxSteps > 20) {
      throw new IllegalArgumentException("A run takes between 1 and 20 steps");
    }
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("A run needs a positive timeout");
    }
  }
}
