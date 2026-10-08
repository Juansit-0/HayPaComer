package dev.haypacomer.application.planning;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.recipe.Recipe;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ListRecipes {

  private final GetHousehold households;
  private final RecipeRepository recipes;

  public ListRecipes(HouseholdRepository households, RecipeRepository recipes) {
    this.households = new GetHousehold(households);
    this.recipes = Objects.requireNonNull(recipes, "recipes");
  }

  public List<Recipe> list(UserId actor, HouseholdId householdId) {
    households.get(actor, householdId);
    return recipes.findByHousehold(householdId).stream()
        .sorted(Comparator.comparing(Recipe::name))
        .toList();
  }
}
