package dev.haypacomer.application.ai;

import java.util.List;
import java.util.Objects;

public record PhotoRecipe(
    String name,
    int servings,
    int minutes,
    List<PhotoIngredient> ingredients,
    List<String> steps,
    double confidence,
    AdvisorSource source) {

  public PhotoRecipe {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(source, "source");
    ingredients = List.copyOf(ingredients);
    steps = List.copyOf(steps);
  }
}
