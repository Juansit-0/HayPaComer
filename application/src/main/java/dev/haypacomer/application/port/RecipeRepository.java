package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository {

  void save(HouseholdId household, Recipe recipe);

  Optional<Recipe> find(HouseholdId household, RecipeId id);

  List<Recipe> findByHousehold(HouseholdId household);
}
