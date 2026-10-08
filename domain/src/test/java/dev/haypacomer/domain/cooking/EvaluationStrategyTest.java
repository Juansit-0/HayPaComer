package dev.haypacomer.domain.cooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.substitution.Substitution;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EvaluationStrategyTest {

  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata TUNA = food("Tuna", FoodCategory.FISH);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);
  private static final FoodMetadata SOY = food("Soy sauce", FoodCategory.CONDIMENT);

  private static final Recipe RICE_WITH_CHICKEN =
      new Recipe(
          RecipeId.newId(),
          "Rice with chicken",
          2,
          35,
          RecipeSource.MANUAL,
          List.of(
              RecipeRequirement.of(CHICKEN, Grams.of(200)),
              RecipeRequirement.of(RICE, Grams.of(150)),
              RecipeRequirement.optional(SOY, Grams.of(15))),
          List.of());

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static Availability stock(long chicken, long rice, long tuna) {
    return new Availability()
        .add(CHICKEN, Grams.of(chicken))
        .add(RICE, Grams.of(rice))
        .add(TUNA, Grams.of(tuna));
  }

  private static RequirementVerdict verdictOf(RecipeEvaluation evaluation, FoodMetadata food) {
    return evaluation.requirements().stream()
        .filter(requirement -> requirement.requirement().food().equals(food))
        .findFirst()
        .orElseThrow()
        .verdict();
  }

  @Test
  void everyStrategyAgreesWhenThereIsEnough() {
    Availability plenty = stock(250, 300, 0);

    for (EvaluationStrategy strategy :
        List.of(
            new StrictStrategy(),
            FlexibleStrategy.standard(),
            new RescueStrategy(
                SubstitutionCatalog.EMPTY, DiningGroup.of(), new StrictStrategy()))) {
      RecipeEvaluation evaluation = strategy.evaluate(RICE_WITH_CHICKEN, plenty, 2);
      assertEquals(RequirementVerdict.ENOUGH, evaluation.verdict());
      assertEquals(2, evaluation.achievableServings());
      assertTrue(evaluation.cookable());
    }
  }

  @Test
  void strictReportsTheMissingGrams() {
    RecipeEvaluation evaluation =
        new StrictStrategy().evaluate(RICE_WITH_CHICKEN, stock(80, 300, 0), 2);

    assertEquals(RequirementVerdict.MISSING, evaluation.verdict());
    assertEquals(0, evaluation.achievableServings());
    assertEquals(Grams.of(120), evaluation.missing().getFirst().shortfall());
    assertEquals(RequirementVerdict.ENOUGH, verdictOf(evaluation, SOY));
    assertFalse(evaluation.cookable());
  }

  @Test
  void flexibleReducesToOnePortionWithinTheCookingTolerance() {
    RecipeEvaluation evaluation =
        FlexibleStrategy.standard().evaluate(RICE_WITH_CHICKEN, stock(80, 300, 0), 2);

    assertEquals(RequirementVerdict.REDUCE, evaluation.verdict());
    assertEquals(1, evaluation.achievableServings());
    assertEquals(Grams.of(100), evaluation.recipe().requirementFor(CHICKEN).orElseThrow().grams());
    assertEquals(RequirementVerdict.REDUCE, verdictOf(evaluation, CHICKEN));
    assertEquals(RequirementVerdict.ENOUGH, verdictOf(evaluation, RICE));
  }

  @Test
  void flexibleGivesUpWhenEvenOnePortionIsFarOff() {
    RecipeEvaluation evaluation =
        FlexibleStrategy.standard().evaluate(RICE_WITH_CHICKEN, stock(60, 300, 0), 2);

    assertEquals(RequirementVerdict.MISSING, evaluation.verdict());
    assertEquals(Grams.of(140), evaluation.missing().getFirst().shortfall());
  }

  @Test
  void rescueUsesAnAllowedSubstituteThatIsInStock() {
    RescueStrategy rescue =
        new RescueStrategy(
            new SubstitutionCatalog(List.of(SubstitutionRule.of(CHICKEN, TUNA, "1.1", 200))),
            DiningGroup.of(),
            FlexibleStrategy.standard());

    RecipeEvaluation withTuna = rescue.evaluate(RICE_WITH_CHICKEN, stock(80, 300, 132), 2);
    RecipeEvaluation notEnoughTuna = rescue.evaluate(RICE_WITH_CHICKEN, stock(80, 300, 131), 2);

    assertEquals(RequirementVerdict.SUBSTITUTE, withTuna.verdict());
    Substitution proposal = withTuna.requirements().getFirst().proposal().orElseThrow();
    assertEquals(TUNA, proposal.substitute());
    assertEquals(Grams.of(120), proposal.replaced());
    assertEquals(Grams.of(132), proposal.substituteGrams());
    assertEquals(2, withTuna.achievableServings());
    assertEquals(RequirementVerdict.REDUCE, notEnoughTuna.verdict());
  }

  @Test
  void rescueDoesNotSpendStockTheRecipeAlreadyNeeds() {
    RescueStrategy rescue =
        new RescueStrategy(
            new SubstitutionCatalog(List.of(SubstitutionRule.of(CHICKEN, RICE, "1", 500))),
            DiningGroup.of(),
            new StrictStrategy());

    assertEquals(
        RequirementVerdict.MISSING,
        rescue.evaluate(RICE_WITH_CHICKEN, stock(80, 200, 0), 2).verdict());
    assertEquals(
        RequirementVerdict.SUBSTITUTE,
        rescue.evaluate(RICE_WITH_CHICKEN, stock(80, 270, 0), 2).verdict());
  }

  @Test
  void rejectsInvalidInputs() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new StrictStrategy().evaluate(RICE_WITH_CHICKEN, stock(1, 1, 1), 0));
    assertThrows(IllegalArgumentException.class, () -> new FlexibleStrategy(BigDecimal.ONE));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RequirementEvaluation(
                RICE_WITH_CHICKEN.requirements().getFirst(),
                Grams.ZERO,
                Grams.ZERO,
                RequirementVerdict.SUBSTITUTE,
                null));
  }
}
