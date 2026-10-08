package dev.haypacomer.application.ai;

import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.food.FoodMetadata;
import java.util.List;
import java.util.Objects;

public record Suggestion(
    RecipeEvaluation evaluation, List<FoodMetadata> rescuedFoods, int score, String reason) {

  public Suggestion {
    Objects.requireNonNull(evaluation, "evaluation");
    Objects.requireNonNull(reason, "reason");
    rescuedFoods = List.copyOf(rescuedFoods);
  }
}
