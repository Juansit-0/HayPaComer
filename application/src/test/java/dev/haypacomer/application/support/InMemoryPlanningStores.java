package dev.haypacomer.application.support;

import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.application.port.WeeklyPlanRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.planning.PlanEntryId;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryPlanningStores {

  private final Map<RecipeId, HouseholdId> recipeOwners = new LinkedHashMap<>();
  private final Map<RecipeId, Recipe> recipeMap = new LinkedHashMap<>();
  public final List<WeeklyPlan> planList = new ArrayList<>();

  public final RecipeRepository recipes =
      new RecipeRepository() {
        @Override
        public void save(HouseholdId household, Recipe recipe) {
          recipeOwners.put(recipe.id(), household);
          recipeMap.put(recipe.id(), recipe);
        }

        @Override
        public Optional<Recipe> find(HouseholdId household, RecipeId id) {
          return Optional.ofNullable(recipeMap.get(id))
              .filter(recipe -> household.equals(recipeOwners.get(id)));
        }

        @Override
        public List<Recipe> findByHousehold(HouseholdId household) {
          return recipeMap.values().stream()
              .filter(recipe -> household.equals(recipeOwners.get(recipe.id())))
              .toList();
        }
      };

  public final WeeklyPlanRepository plans =
      new WeeklyPlanRepository() {
        @Override
        public void save(WeeklyPlan plan) {
          planList.removeIf(
              existing ->
                  existing.id().equals(plan.id())
                      || existing.household().equals(plan.household())
                          && existing.weekStart().equals(plan.weekStart()));
          planList.add(plan);
        }

        @Override
        public Optional<WeeklyPlan> current(HouseholdId household, LocalDate today) {
          return planList.stream()
              .filter(plan -> plan.household().equals(household))
              .filter(plan -> plan.covers(today))
              .max(Comparator.comparing(WeeklyPlan::weekStart));
        }

        @Override
        public Optional<WeeklyPlan> findByEntry(HouseholdId household, PlanEntryId entry) {
          return planList.stream()
              .filter(plan -> plan.household().equals(household))
              .filter(plan -> plan.entry(entry).isPresent())
              .findFirst();
        }
      };
}
