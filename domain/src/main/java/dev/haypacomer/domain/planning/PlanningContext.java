package dev.haypacomer.domain.planning;

import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.recipe.Recipe;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PlanningContext(
    List<Recipe> recipes,
    Availability availability,
    Set<String> atRiskFoods,
    DiningGroup diners,
    int servings) {

  public PlanningContext {
    Objects.requireNonNull(availability, "availability");
    Objects.requireNonNull(diners, "diners");
    recipes = List.copyOf(recipes);
    atRiskFoods = Set.copyOf(atRiskFoods);
    if (servings < 1) {
      throw new IllegalArgumentException("Servings must be at least 1: " + servings);
    }
  }
}
