package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class RescueStrategy implements EvaluationStrategy {

  private final Function<FoodMetadata, List<FoodMetadata>> substitutes;
  private final EvaluationStrategy fallback;

  public RescueStrategy(
      Function<FoodMetadata, List<FoodMetadata>> substitutes, EvaluationStrategy fallback) {
    this.substitutes = Objects.requireNonNull(substitutes, "substitutes");
    this.fallback = Objects.requireNonNull(fallback, "fallback");
  }

  @Override
  public RecipeEvaluation evaluate(Recipe recipe, Availability availability, int servings) {
    Recipe scaled = Requirements.at(recipe, servings);
    List<RequirementEvaluation> evaluations = new ArrayList<>();
    for (RequirementEvaluation evaluation : Requirements.plain(scaled, availability)) {
      if (evaluation.verdict() != RequirementVerdict.MISSING) {
        evaluations.add(evaluation);
        continue;
      }
      Optional<FoodMetadata> substitute = substituteFor(evaluation.requirement(), availability);
      if (substitute.isEmpty()) {
        return fallback.evaluate(recipe, availability, servings);
      }
      evaluations.add(
          new RequirementEvaluation(
              evaluation.requirement(),
              evaluation.available(),
              evaluation.shortfall(),
              RequirementVerdict.SUBSTITUTE,
              substitute.get()));
    }
    return new RecipeEvaluation(
        scaled, servings, servings, Requirements.overall(evaluations), evaluations);
  }

  private Optional<FoodMetadata> substituteFor(
      RecipeRequirement requirement, Availability availability) {
    Grams needed = requirement.grams();
    return substitutes.apply(requirement.food()).stream()
        .filter(candidate -> availability.of(candidate).isAtLeast(needed))
        .findFirst();
  }
}
