package dev.haypacomer.domain.cooking;

import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FlexibleStrategy implements EvaluationStrategy {

  public static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("0.25");

  private final BigDecimal tolerance;

  public FlexibleStrategy(BigDecimal tolerance) {
    this.tolerance = Objects.requireNonNull(tolerance, "tolerance");
    if (tolerance.signum() < 0 || tolerance.compareTo(BigDecimal.ONE) >= 0) {
      throw new IllegalArgumentException("Tolerance must be between 0 and 1");
    }
  }

  public static FlexibleStrategy standard() {
    return new FlexibleStrategy(DEFAULT_TOLERANCE);
  }

  @Override
  public RecipeEvaluation evaluate(Recipe recipe, Availability availability, int servings) {
    Recipe requested = Requirements.at(recipe, servings);
    if (Requirements.overall(Requirements.plain(requested, availability))
        == RequirementVerdict.ENOUGH) {
      return new StrictStrategy().evaluate(recipe, availability, servings);
    }
    for (int portions = servings; portions >= 1; portions--) {
      Recipe scaled = Requirements.at(recipe, portions);
      if (fits(scaled, availability)) {
        return reduced(scaled, availability, servings, portions);
      }
    }
    return new StrictStrategy().evaluate(recipe, availability, servings);
  }

  private boolean fits(Recipe scaled, Availability availability) {
    return scaled.mandatoryRequirements().stream()
        .allMatch(
            requirement ->
                Requirements.covers(
                    availability.of(requirement.food()), requirement.grams(), tolerance));
  }

  private RecipeEvaluation reduced(
      Recipe scaled, Availability availability, int requested, int portions) {
    List<RequirementEvaluation> evaluations = new ArrayList<>();
    for (RecipeRequirement requirement : scaled.requirements()) {
      Grams available = availability.of(requirement.food());
      Grams shortfall = available.shortfallTo(requirement.grams());
      RequirementVerdict verdict =
          shortfall.isZero() || requirement.optional() && portions == requested
              ? RequirementVerdict.ENOUGH
              : RequirementVerdict.REDUCE;
      evaluations.add(new RequirementEvaluation(requirement, available, shortfall, verdict, null));
    }
    RequirementVerdict overall =
        portions < requested ? RequirementVerdict.REDUCE : Requirements.overall(evaluations);
    return new RecipeEvaluation(scaled, requested, portions, overall, evaluations);
  }
}
