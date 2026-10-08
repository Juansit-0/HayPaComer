package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import java.util.Objects;
import java.util.Optional;

public record RequirementEvaluation(
    RecipeRequirement requirement,
    Grams available,
    Grams shortfall,
    RequirementVerdict verdict,
    FoodMetadata substitute) {

  public RequirementEvaluation {
    Objects.requireNonNull(requirement, "requirement");
    Objects.requireNonNull(available, "available");
    Objects.requireNonNull(shortfall, "shortfall");
    Objects.requireNonNull(verdict, "verdict");
    if ((verdict == RequirementVerdict.SUBSTITUTE) != (substitute != null)) {
      throw new IllegalArgumentException("Only a SUBSTITUTE verdict carries a substitute");
    }
  }

  public Optional<FoodMetadata> substituteFood() {
    return Optional.ofNullable(substitute);
  }
}
