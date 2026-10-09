package dev.haypacomer.domain.coldchain.investigation;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Objects;

public record FoodAssessment(
    FoodItemId item, String food, Grams grams, FoodVerdict verdict, String reason) {

  public FoodAssessment {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(grams, "grams");
    Objects.requireNonNull(verdict, "verdict");
    Objects.requireNonNull(reason, "reason");
  }
}
