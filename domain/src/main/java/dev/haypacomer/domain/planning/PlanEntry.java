package dev.haypacomer.domain.planning;

import dev.haypacomer.domain.recipe.Recipe;
import java.util.Objects;

public record PlanEntry(
    PlanEntryId id, int day, Meal meal, Recipe recipe, int servings, boolean needsShopping) {

  public static final int DAYS = 7;

  public PlanEntry {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(meal, "meal");
    Objects.requireNonNull(recipe, "recipe");
    if (day < 1 || day > DAYS) {
      throw new IllegalArgumentException("Day must be between 1 and " + DAYS + ": " + day);
    }
    if (servings < 1) {
      throw new IllegalArgumentException("Servings must be at least 1: " + servings);
    }
  }

  public static PlanEntry of(int day, Meal meal, Recipe recipe, int servings, boolean shopping) {
    return new PlanEntry(PlanEntryId.newId(), day, meal, recipe, servings, shopping);
  }

  public int slot() {
    return (day - 1) * Meal.values().length + meal.ordinal();
  }
}
