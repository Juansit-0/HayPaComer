package dev.haypacomer.domain.recipe;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.util.Objects;

public record StepWeighing(FoodMetadata food, Grams target) {

  public StepWeighing {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(target, "target");
    if (target.isZero()) {
      throw new IllegalArgumentException(
          "Weighing target for " + food.name() + " must be positive");
    }
  }

  public StepWeighing scaledBy(BigDecimal factor) {
    return new StepWeighing(food, target.times(factor));
  }
}
