package dev.haypacomer.application.market;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Objects;

public record MissingItem(FoodMetadata food, Grams shortfall, Grams added, Grams pending) {

  public MissingItem {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(shortfall, "shortfall");
    Objects.requireNonNull(added, "added");
    Objects.requireNonNull(pending, "pending");
  }
}
