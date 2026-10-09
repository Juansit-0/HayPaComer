package dev.haypacomer.application.agent;

import java.util.Objects;
import java.util.UUID;

public record ConversationId(UUID value) {

  public ConversationId {
    Objects.requireNonNull(value, "value");
  }

  public static ConversationId newId() {
    return new ConversationId(UUID.randomUUID());
  }
}
