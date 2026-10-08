package dev.haypacomer.application.agent;

import java.util.Objects;
import java.util.UUID;

public record AgentRunId(UUID value) {

  public AgentRunId {
    Objects.requireNonNull(value, "value");
  }

  public static AgentRunId newId() {
    return new AgentRunId(UUID.randomUUID());
  }
}
