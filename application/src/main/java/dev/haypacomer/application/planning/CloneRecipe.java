package dev.haypacomer.application.planning;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.application.port.RecipeTemplateRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.recipe.ClonedRecipe;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import java.util.Objects;

public final class CloneRecipe {

  private final GetHousehold households;
  private final RecipeRepository recipes;
  private final RecipeTemplateRepository templates;

  public CloneRecipe(
      HouseholdRepository households,
      RecipeRepository recipes,
      RecipeTemplateRepository templates) {
    this.households = new GetHousehold(households);
    this.recipes = Objects.requireNonNull(recipes, "recipes");
    this.templates = Objects.requireNonNull(templates, "templates");
  }

  public ClonedRecipe clone(UserId actor, HouseholdId householdId, RecipeId sourceId) {
    households.get(actor, householdId).requirePermission(actor, Permission.COOK);
    Recipe source =
        recipes
            .find(householdId, sourceId)
            .or(() -> templates.template(sourceId))
            .orElseThrow(RecipeNotFoundException::new);
    ClonedRecipe copy = ClonedRecipe.of(source);
    recipes.saveCopy(householdId, copy);
    return copy;
  }
}
