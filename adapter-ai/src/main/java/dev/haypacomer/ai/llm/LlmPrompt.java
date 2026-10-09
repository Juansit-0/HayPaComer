package dev.haypacomer.ai.llm;

import java.util.Objects;
import java.util.Optional;

public record LlmPrompt(String system, String user, LlmImage image) {

  public LlmPrompt {
    Objects.requireNonNull(system, "system");
    Objects.requireNonNull(user, "user");
  }

  public LlmPrompt(String system, String user) {
    this(system, user, null);
  }

  public Optional<LlmImage> attachedImage() {
    return Optional.ofNullable(image);
  }
}
