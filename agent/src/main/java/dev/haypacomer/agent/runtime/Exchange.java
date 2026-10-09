package dev.haypacomer.agent.runtime;

import java.util.Objects;

public record Exchange(Decision.CallTool call, Observation observation) {

  public Exchange {
    Objects.requireNonNull(call, "call");
    Objects.requireNonNull(observation, "observation");
  }
}
