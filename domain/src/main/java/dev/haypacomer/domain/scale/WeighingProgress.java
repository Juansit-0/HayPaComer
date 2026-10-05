package dev.haypacomer.domain.scale;

import dev.haypacomer.domain.quantity.Grams;
import java.util.Objects;

public record WeighingProgress(
    String food,
    Grams measured,
    Grams target,
    Grams remaining,
    int percent,
    WeighingStatus status) {

  public WeighingProgress {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(measured, "measured");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(remaining, "remaining");
    Objects.requireNonNull(status, "status");
  }
}
