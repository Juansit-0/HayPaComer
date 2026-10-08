package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.recipe.Recipe;
import java.util.List;
import java.util.Objects;

public record RecipeEvaluation(
    Recipe recipe,
    int requestedServings,
    int achievableServings,
    RequirementVerdict verdict,
    List<RequirementEvaluation> requirements) {

  public RecipeEvaluation {
    Objects.requireNonNull(recipe, "recipe");
    Objects.requireNonNull(verdict, "verdict");
    requirements = List.copyOf(requirements);
  }

  public List<RequirementEvaluation> missing() {
    return requirements.stream()
        .filter(requirement -> requirement.verdict() == RequirementVerdict.MISSING)
        .toList();
  }

  public boolean cookable() {
    return verdict != RequirementVerdict.MISSING;
  }
}
