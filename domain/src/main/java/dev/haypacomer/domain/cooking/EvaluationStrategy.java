package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.recipe.Recipe;

public interface EvaluationStrategy {

  RecipeEvaluation evaluate(Recipe recipe, Availability availability, int servings);
}
