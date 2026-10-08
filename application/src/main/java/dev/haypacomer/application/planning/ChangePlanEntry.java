package dev.haypacomer.application.planning;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.cooking.StrategyKind;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.application.port.WeeklyPlanRepository;
import dev.haypacomer.domain.cooking.RequirementVerdict;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.planning.PlanEntry;
import dev.haypacomer.domain.planning.PlanEntryId;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import java.util.Objects;
import java.util.Set;

public final class ChangePlanEntry {

  private final GetHousehold households;
  private final RecipeRepository recipes;
  private final WeeklyPlanRepository plans;
  private final EvaluateRecipe evaluate;

  public ChangePlanEntry(
      HouseholdRepository households,
      RecipeRepository recipes,
      WeeklyPlanRepository plans,
      EvaluateRecipe evaluate) {
    this.households = new GetHousehold(households);
    this.recipes = Objects.requireNonNull(recipes, "recipes");
    this.plans = Objects.requireNonNull(plans, "plans");
    this.evaluate = Objects.requireNonNull(evaluate, "evaluate");
  }

  public PlanEntry change(
      UserId actor, HouseholdId householdId, PlanEntryId entryId, RecipeId recipeId, int servings) {
    households.get(actor, householdId).requirePermission(actor, Permission.COOK);
    WeeklyPlan plan =
        plans.findByEntry(householdId, entryId).orElseThrow(WeeklyPlanNotFoundException::new);
    Recipe recipe = recipes.find(householdId, recipeId).orElseThrow(RecipeNotFoundException::new);
    boolean shopping =
        evaluate
                .evaluate(actor, householdId, recipe, servings, StrategyKind.STRICT, Set.of())
                .verdict()
            != RequirementVerdict.ENOUGH;
    PlanEntry changed = plan.change(entryId, recipe, servings, shopping);
    plans.save(plan);
    return changed;
  }
}
