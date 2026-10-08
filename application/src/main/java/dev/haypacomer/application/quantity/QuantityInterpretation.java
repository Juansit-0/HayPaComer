package dev.haypacomer.application.quantity;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.QuantityExpression;
import java.util.Objects;

public record QuantityInterpretation(
    FoodMetadata food, QuantityExpression expression, Grams grams) {

  public QuantityInterpretation {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(grams, "grams");
  }
}
