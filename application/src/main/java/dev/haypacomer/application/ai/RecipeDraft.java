package dev.haypacomer.application.ai;

import java.util.List;
import java.util.Objects;

public record RecipeDraft(
    String name,
    int servings,
    int minutes,
    List<DraftIngredient> ingredients,
    List<String> steps,
    double confidence,
    AdvisorSource source) {

  public RecipeDraft {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(source, "source");
    ingredients = List.copyOf(ingredients);
    steps = List.copyOf(steps);
  }

  public boolean verified() {
    return !ingredients.isEmpty() && ingredients.stream().allMatch(DraftIngredient::verified);
  }
}
