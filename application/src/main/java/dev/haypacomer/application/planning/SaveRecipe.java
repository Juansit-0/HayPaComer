package dev.haypacomer.application.planning;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.recipe.Recipe;
import java.util.Objects;

public final class SaveRecipe {

  private final GetHousehold households;
  private final RecipeRepository recipes;

  public SaveRecipe(HouseholdRepository households, RecipeRepository recipes) {
    this.households = new GetHousehold(households);
    this.recipes = Objects.requireNonNull(recipes, "recipes");
  }

  public Recipe save(UserId actor, HouseholdId householdId, Recipe recipe) {
    households.get(actor, householdId).requirePermission(actor, Permission.COOK);
    recipes.save(householdId, recipe);
    return recipe;
  }
}
