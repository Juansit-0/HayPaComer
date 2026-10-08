package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.substitution.Substitution;
import java.util.Objects;
import java.util.Optional;

public record RequirementEvaluation(
    RecipeRequirement requirement,
    Grams available,
    Grams shortfall,
    RequirementVerdict verdict,
    Substitution substitution) {

  public RequirementEvaluation {
    Objects.requireNonNull(requirement, "requirement");
    Objects.requireNonNull(available, "available");
    Objects.requireNonNull(shortfall, "shortfall");
    Objects.requireNonNull(verdict, "verdict");
    if ((verdict == RequirementVerdict.SUBSTITUTE) != (substitution != null)) {
      throw new IllegalArgumentException("Only a SUBSTITUTE verdict carries a substitution");
    }
  }

  public Optional<Substitution> proposal() {
    return Optional.ofNullable(substitution);
  }
}
