package dev.haypacomer.application.agent;

import java.time.Instant;
import java.util.Objects;

public record ConversationSummary(ConversationId id, Instant lastActivity) {

  public ConversationSummary {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(lastActivity, "lastActivity");
  }
}
