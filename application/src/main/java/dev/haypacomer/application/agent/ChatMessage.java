package dev.haypacomer.application.agent;

import java.time.Instant;
import java.util.Objects;

public record ChatMessage(ChatRole role, String text, Instant at) {

  public static final int MAX_LENGTH = 4_000;

  public ChatMessage {
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(at, "at");
    if (text.isBlank()) {
      throw new IllegalArgumentException("A message needs text");
    }
    if (text.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("A message has at most " + MAX_LENGTH + " characters");
    }
  }
}
