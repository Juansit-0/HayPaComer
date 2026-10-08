package dev.haypacomer.domain.cooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.QuantityParser;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.StepTimer;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import dev.haypacomer.domain.substitution.SubstitutionProblem;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CookingBoundariesTest {

  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata TUNA = food("Tuna", FoodCategory.FISH);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);
  private static final Recipe RECIPE =
      new Recipe(
          RecipeId.newId(),
          "Rice with chicken",
          2,
          35,
          RecipeSource.MANUAL,
          List.of(
              RecipeRequirement.of(CHICKEN, Grams.of(200)),
              RecipeRequirement.of(RICE, Grams.of(150))),
          List.of(
              RecipeStep.of(1, "Weigh").withWeighing(new StepWeighing(CHICKEN, Grams.of(200))),
              RecipeStep.of(2, "Cook").withTimer(Duration.ofMinutes(10))));

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

  @Test
  void oddServingsScaleEveryRequirementAndWeighingTarget() {
    Recipe forThree = RECIPE.scaledTo(3);

    assertEquals(Grams.of(300), forThree.requirementFor(CHICKEN).orElseThrow().grams());
    assertEquals(Grams.of(225), forThree.requirementFor(RICE).orElseThrow().grams());
    assertEquals(
        Grams.of(300), forThree.steps().getFirst().weighingTarget().orElseThrow().target());
    assertEquals(
        Grams.of(300),
        RECIPE.scaledTo(1).scaledTo(3).requirementFor(CHICKEN).orElseThrow().grams());
  }

  @Test
  void theCookingToleranceEndsExactlyAtAQuarter() {
    RecipeEvaluation atTheEdge =
        FlexibleStrategy.standard().evaluate(RECIPE, stock(150, 150, 0), 2);
    RecipeEvaluation justBelow =
        FlexibleStrategy.standard().evaluate(RECIPE, stock(149, 150, 0), 2);

    assertEquals(2, atTheEdge.achievableServings());
    assertEquals(RequirementVerdict.REDUCE, atTheEdge.verdict());
    assertEquals(Grams.of(50), atTheEdge.requirements().getFirst().shortfall());
    assertEquals(1, justBelow.achievableServings());
    assertEquals(RequirementVerdict.REDUCE, justBelow.verdict());
    assertEquals(
        RequirementVerdict.MISSING,
        FlexibleStrategy.standard().evaluate(RECIPE, stock(74, 150, 0), 2).verdict());
  }

  @Test
  void theSubstitutionLimitIncludesItsOwnValue() {
    SubstitutionRule rule = SubstitutionRule.of(CHICKEN, TUNA, "1", 120);
    SubstitutionCatalog catalog = new SubstitutionCatalog(List.of(rule));

    assertTrue(catalog.check(rule, Grams.of(120), Grams.of(120), DiningGroup.of()).isEmpty());
    assertEquals(
        Set.of(SubstitutionProblem.OVER_LIMIT, SubstitutionProblem.NOT_ENOUGH_STOCK),
        catalog.check(rule, Grams.of(121), Grams.of(120), DiningGroup.of()));
    assertEquals(
        RequirementVerdict.SUBSTITUTE,
        new RescueStrategy(catalog, DiningGroup.of(), new StrictStrategy())
            .evaluate(RECIPE, stock(80, 150, 120), 2)
            .verdict());
  }

  @Test
  void aTimerPausedAtZeroOnlyRingsAfterResuming() {
    Instant start = Instant.parse("2026-10-08T19:00:00Z");
    StepTimer paused =
        StepTimer.start(CookingSessionId.newId(), 2, Duration.ofMinutes(10), start)
            .pause(start.plusSeconds(600));

    assertEquals(Duration.ZERO, paused.remaining(start.plusSeconds(900)));
    assertFalse(paused.due(start.plusSeconds(900)));
    assertTrue(paused.resume(start.plusSeconds(900)).due(start.plusSeconds(900)));
  }

  @Test
  void quantitiesWrittenInTheRecipeMatchTheScaleGrams() {
    assertEquals(
        Grams.of(200), QuantityParser.parse("150 g + 50 g").interpret(ConversionFactors.MASS_ONLY));
    assertEquals(
        Grams.of(150), QuantityParser.parse("0,15 kg").interpret(ConversionFactors.MASS_ONLY));
  }
}
