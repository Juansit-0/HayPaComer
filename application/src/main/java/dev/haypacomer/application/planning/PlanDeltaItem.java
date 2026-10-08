package dev.haypacomer.application.planning;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Objects;

public record PlanDeltaItem(
    FoodMetadata food, Grams needed, Grams available, Grams added, Grams pending) {

  public PlanDeltaItem {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(needed, "needed");
    Objects.requireNonNull(available, "available");
    Objects.requireNonNull(added, "added");
    Objects.requireNonNull(pending, "pending");
  }
}
