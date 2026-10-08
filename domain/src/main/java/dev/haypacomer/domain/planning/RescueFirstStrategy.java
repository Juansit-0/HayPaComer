package dev.haypacomer.domain.planning;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RescueFirstStrategy implements PlanningStrategy {

  private static final int RESCUE_POINTS = 100;
  private static final int COVERED_POINTS = 50;

  @Override
  public List<PlanEntry> plan(PlanningContext context) {
    List<Recipe> shareable =
        context.recipes().stream().filter(recipe -> context.diners().canShare(recipe)).toList();
    if (shareable.isEmpty()) {
      throw new IllegalArgumentException("No saved recipe suits everyone who is eating");
    }
    Map<String, Grams> used = new HashMap<>();
    List<PlanEntry> entries = new ArrayList<>();
    Set<RecipeId> yesterday = Set.of();
    for (int day = 1; day <= PlanEntry.DAYS; day++) {
      Set<RecipeId> today = new HashSet<>();
      for (Meal meal : Meal.values()) {
        Recipe chosen = choose(shareable, context, used, yesterday, today);
        Recipe scaled = chosen.scaledTo(context.servings());
        boolean shopping = !covered(scaled, context, used);
        consume(scaled, context, used);
        entries.add(PlanEntry.of(day, meal, chosen, context.servings(), shopping));
        today.add(chosen.id());
      }
      yesterday = today;
    }
    return List.copyOf(entries);
  }

  private static Recipe choose(
      List<Recipe> recipes,
      PlanningContext context,
      Map<String, Grams> used,
      Set<RecipeId> yesterday,
      Set<RecipeId> today) {
    List<Recipe> fresh =
        recipes.stream()
            .filter(recipe -> !yesterday.contains(recipe.id()) && !today.contains(recipe.id()))
            .toList();
    List<Recipe> notToday =
        recipes.stream().filter(recipe -> !today.contains(recipe.id())).toList();
    List<Recipe> pool = !fresh.isEmpty() ? fresh : !notToday.isEmpty() ? notToday : recipes;
    return pool.stream()
        .sorted(
            Comparator.comparingInt(
                    (Recipe recipe) -> score(recipe.scaledTo(context.servings()), context, used))
                .reversed()
                .thenComparing(Recipe::name))
        .findFirst()
        .orElseThrow();
  }

  private static int score(Recipe scaled, PlanningContext context, Map<String, Grams> used) {
    int rescued =
        (int)
            scaled.mandatoryRequirements().stream()
                .map(RecipeRequirement::food)
                .filter(food -> context.atRiskFoods().contains(food.key()))
                .filter(food -> !remaining(food, context, used).isZero())
                .count();
    return rescued * RESCUE_POINTS + (covered(scaled, context, used) ? COVERED_POINTS : 0);
  }

  private static boolean covered(Recipe scaled, PlanningContext context, Map<String, Grams> used) {
    return scaled.mandatoryRequirements().stream()
        .allMatch(
            requirement ->
                remaining(requirement.food(), context, used).isAtLeast(requirement.grams()));
  }

  private static void consume(Recipe scaled, PlanningContext context, Map<String, Grams> used) {
    for (RecipeRequirement requirement : scaled.requirements()) {
      Grams left = remaining(requirement.food(), context, used);
      Grams taken = left.isAtLeast(requirement.grams()) ? requirement.grams() : left;
      used.merge(requirement.food().key(), taken, Grams::plus);
    }
  }

  private static Grams remaining(
      FoodMetadata food, PlanningContext context, Map<String, Grams> used) {
    return context.availability().of(food).minus(used.getOrDefault(food.key(), Grams.ZERO));
  }
}
