package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.recipe.Recipe;
import java.util.List;

public final class StrictStrategy implements EvaluationStrategy {

  @Override
  public RecipeEvaluation evaluate(Recipe recipe, Availability availability, int servings) {
    Recipe scaled = Requirements.at(recipe, servings);
    List<RequirementEvaluation> evaluations = Requirements.plain(scaled, availability);
    RequirementVerdict verdict = Requirements.overall(evaluations);
    return new RecipeEvaluation(
        scaled,
        servings,
        verdict == RequirementVerdict.ENOUGH ? servings : 0,
        verdict,
        evaluations);
  }
}
