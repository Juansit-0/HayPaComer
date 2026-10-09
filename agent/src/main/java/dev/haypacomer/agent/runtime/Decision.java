package dev.haypacomer.agent.runtime;

import java.util.Map;
import java.util.Objects;

public sealed interface Decision {

  record CallTool(String tool, Map<String, String> arguments, String reason) implements Decision {

    public CallTool {
      Objects.requireNonNull(tool, "tool");
      Objects.requireNonNull(reason, "reason");
      arguments = Map.copyOf(arguments);
    }
  }

  record Defer(String reason) implements Decision {

    public Defer {
      Objects.requireNonNull(reason, "reason");
    }
  }

  record FinalAnswer(String text) implements Decision {

    public FinalAnswer {
      Objects.requireNonNull(text, "text");
      if (text.isBlank()) {
        throw new IllegalArgumentException("An answer cannot be blank");
      }
    }
  }
}
