package dev.haypacomer.agent.runtime;

import java.util.Objects;

public record Observation(String tool, String content, boolean failed) {

  public static final int MAX_LENGTH = 4000;

  public Observation {
    Objects.requireNonNull(tool, "tool");
    Objects.requireNonNull(content, "content");
    if (content.length() > MAX_LENGTH) {
      content = content.substring(0, MAX_LENGTH);
    }
  }

  public static Observation of(String tool, String content) {
    return new Observation(tool, content, false);
  }

  public static Observation failure(String tool, String reason) {
    return new Observation(tool, reason, true);
  }
}
