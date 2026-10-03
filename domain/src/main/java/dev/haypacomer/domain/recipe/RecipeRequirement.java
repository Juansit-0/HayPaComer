package dev.haypacomer.domain.recipe;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.util.Objects;

public record RecipeRequirement(FoodMetadata food, Grams grams, boolean optional) {

  public RecipeRequirement {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(grams, "grams");
    if (grams.isZero()) {
      throw new IllegalArgumentException("Requirement for " + food.name() + " must be positive");
    }
  }

  public static RecipeRequirement of(FoodMetadata food, Grams grams) {
    return new RecipeRequirement(food, grams, false);
  }

  public static RecipeRequirement optional(FoodMetadata food, Grams grams) {
    return new RecipeRequirement(food, grams, true);
  }

  public RecipeRequirement scaledBy(BigDecimal factor) {
    return new RecipeRequirement(food, grams.times(factor), optional);
  }
}
