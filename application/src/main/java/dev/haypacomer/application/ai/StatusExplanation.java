package dev.haypacomer.application.ai;

import java.util.Objects;

public record StatusExplanation(
    String food, Urgency urgency, String message, AdvisorSource source) {

  public StatusExplanation {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(urgency, "urgency");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(source, "source");
  }
}
