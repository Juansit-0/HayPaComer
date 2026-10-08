package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.substitution.Substitution;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RescueStrategy implements EvaluationStrategy {

  private final SubstitutionCatalog catalog;
  private final DiningGroup diners;
  private final EvaluationStrategy fallback;

  public RescueStrategy(
      SubstitutionCatalog catalog, DiningGroup diners, EvaluationStrategy fallback) {
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.diners = Objects.requireNonNull(diners, "diners");
    this.fallback = Objects.requireNonNull(fallback, "fallback");
  }

  @Override
  public RecipeEvaluation evaluate(Recipe recipe, Availability availability, int servings) {
    Recipe scaled = Requirements.at(recipe, servings);
    Map<String, Grams> reserved = new HashMap<>();
    for (RecipeRequirement requirement : scaled.requirements()) {
      reserved.merge(requirement.food().key(), requirement.grams(), Grams::plus);
    }
    List<RequirementEvaluation> evaluations = new ArrayList<>();
    for (RequirementEvaluation evaluation : Requirements.plain(scaled, availability)) {
      if (evaluation.verdict() != RequirementVerdict.MISSING) {
        evaluations.add(evaluation);
        continue;
      }
      Optional<Substitution> substitution =
          catalog.propose(
              evaluation.requirement().food(),
              evaluation.shortfall(),
              food -> remaining(food, availability, reserved),
              diners);
      if (substitution.isEmpty()) {
        return fallback.evaluate(recipe, availability, servings);
      }
      reserved.merge(
          substitution.get().substitute().key(), substitution.get().substituteGrams(), Grams::plus);
      evaluations.add(
          new RequirementEvaluation(
              evaluation.requirement(),
              evaluation.available(),
              evaluation.shortfall(),
              RequirementVerdict.SUBSTITUTE,
              substitution.get()));
    }
    return new RecipeEvaluation(
        scaled, servings, servings, Requirements.overall(evaluations), evaluations);
  }

  private static Grams remaining(
      FoodMetadata food, Availability availability, Map<String, Grams> reserved) {
    Grams available = availability.of(food);
    Grams taken = reserved.getOrDefault(food.key(), Grams.ZERO);
    return available.isAtLeast(taken) ? available.minus(taken) : Grams.ZERO;
  }
}
