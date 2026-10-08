package dev.haypacomer.application.ai;

import dev.haypacomer.domain.quantity.QuantityExpression;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record ParsedIntent(
    IntentAction action,
    String food,
    QuantityExpression quantity,
    LocalDate expiresOn,
    double confidence,
    AdvisorSource source) {

  public ParsedIntent {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(source, "source");
    food = food.strip();
    if (food.isEmpty()) {
      throw new IllegalArgumentException("An intent needs a food");
    }
    if (confidence < 0 || confidence > 1) {
      throw new IllegalArgumentException("Confidence must be between 0 and 1: " + confidence);
    }
  }

  public Optional<QuantityExpression> amount() {
    return Optional.ofNullable(quantity);
  }

  public Optional<LocalDate> expiry() {
    return Optional.ofNullable(expiresOn);
  }
}
