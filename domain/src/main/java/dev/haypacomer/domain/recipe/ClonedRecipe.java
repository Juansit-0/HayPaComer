package dev.haypacomer.domain.recipe;

import java.util.Objects;

public record ClonedRecipe(Recipe recipe, RecipeId clonedFrom) {

  public ClonedRecipe {
    Objects.requireNonNull(recipe, "recipe");
    Objects.requireNonNull(clonedFrom, "clonedFrom");
    if (recipe.id().equals(clonedFrom)) {
      throw new IllegalArgumentException("A copy needs its own id");
    }
  }

  public static ClonedRecipe of(Recipe original) {
    return new ClonedRecipe(original.copy(), original.id());
  }
}
