package dev.haypacomer.ai.llm;

import java.util.Objects;

public record LlmPrompt(String system, String user) {

  public LlmPrompt {
    Objects.requireNonNull(system, "system");
    Objects.requireNonNull(user, "user");
  }
}
