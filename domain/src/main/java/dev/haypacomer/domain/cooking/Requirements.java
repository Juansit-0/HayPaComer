package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class Requirements {

  private Requirements() {}

  static Recipe at(Recipe recipe, int servings) {
    if (servings < 1) {
      throw new IllegalArgumentException("Servings must be at least 1: " + servings);
    }
    return recipe.scaledTo(servings);
  }

  static boolean covers(Grams available, Grams needed, BigDecimal tolerance) {
    return available.isAtLeast(needed.times(BigDecimal.ONE.subtract(tolerance)));
  }

  static List<RequirementEvaluation> plain(Recipe scaled, Availability availability) {
    List<RequirementEvaluation> evaluations = new ArrayList<>();
    for (RecipeRequirement requirement : scaled.requirements()) {
      Grams available = availability.of(requirement.food());
      Grams shortfall = available.shortfallTo(requirement.grams());
      RequirementVerdict verdict =
          shortfall.isZero() || requirement.optional()
              ? RequirementVerdict.ENOUGH
              : RequirementVerdict.MISSING;
      evaluations.add(new RequirementEvaluation(requirement, available, shortfall, verdict, null));
    }
    return evaluations;
  }

  static RequirementVerdict overall(List<RequirementEvaluation> evaluations) {
    return evaluations.stream()
        .map(RequirementEvaluation::verdict)
        .max(Enum::compareTo)
        .orElse(RequirementVerdict.ENOUGH);
  }
}
