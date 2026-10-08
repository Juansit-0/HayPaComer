package dev.haypacomer.domain.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CookingSessionTest {

  private static final Instant T0 = Instant.parse("2026-10-08T18:00:00Z");
  private static final FoodMetadata RICE =
      new FoodMetadata(
          "Rice", FoodCategory.GRAIN, Unit.GRAM, ConversionFactors.MASS_ONLY, false, 365, Set.of());
  private static final Recipe RICE_BOWL =
      new Recipe(
          RecipeId.newId(),
          "Rice bowl",
          2,
          25,
          RecipeSource.MANUAL,
          List.of(RecipeRequirement.of(RICE, Grams.of(150))),
          List.of(
              RecipeStep.of(1, "Weigh the rice")
                  .withWeighing(new StepWeighing(RICE, Grams.of(150))),
              RecipeStep.of(2, "Boil"),
              RecipeStep.of(3, "Serve")));

  private CookingSession start() {
    return CookingSession.start(HouseholdId.newId(), RICE_BOWL, 4, UserId.newId(), T0);
  }

  @Test
  void walksThroughEveryStepUntilFinished() {
    CookingSession session = start();

    assertEquals(SessionPhase.PREPARING, session.phase());
    assertTrue(session.step().isEmpty());
    assertEquals(
        Grams.of(300), session.recipe().steps().getFirst().weighingTarget().orElseThrow().target());

    session.next(T0.plusSeconds(60));
    assertEquals(SessionPhase.COOKING, session.phase());
    assertEquals("Weigh the rice", session.step().orElseThrow().instruction());
    session.next(T0.plusSeconds(120));
    session.next(T0.plusSeconds(180));
    assertEquals(3, session.currentStep());
    session.next(T0.plusSeconds(240));

    assertEquals(SessionPhase.FINISHED, session.phase());
    assertFalse(session.phase().active());
    assertEquals(
        List.of(1, 2, 3), session.completions().stream().map(StepCompletion::position).toList());
    assertEquals(T0.plusSeconds(240), session.updatedAt());
    assertThrows(IllegalSessionTransitionException.class, () -> session.next(T0));
  }

  @Test
  void pausesAndResumesOnTheSameStep() {
    CookingSession session = start();
    session.next(T0);
    session.next(T0.plusSeconds(30));

    session.pause(T0.plusSeconds(40));
    assertEquals(new Paused(2, T0.plusSeconds(40)), session.state());
    assertThrows(IllegalSessionTransitionException.class, () -> session.next(T0));
    assertThrows(IllegalSessionTransitionException.class, () -> session.pause(T0));

    session.resume(T0.plusSeconds(600));
    assertEquals(new Cooking(2), session.state());
    assertThrows(IllegalSessionTransitionException.class, () -> session.resume(T0));
  }

  @Test
  void canBeAbandonedFromAnyActivePhase() {
    CookingSession preparing = start();
    preparing.abandon(T0);
    assertEquals(new Abandoned(0, T0), preparing.state());
    assertThrows(IllegalSessionTransitionException.class, () -> preparing.abandon(T0));

    CookingSession paused = start();
    paused.next(T0);
    paused.pause(T0);
    paused.abandon(T0.plusSeconds(5));
    assertEquals(SessionPhase.ABANDONED, paused.phase());
    assertEquals(1, paused.currentStep());

    CookingSession cooking = start();
    cooking.next(T0);
    cooking.abandon(T0);
    assertEquals(SessionPhase.ABANDONED, cooking.phase());
    assertThrows(IllegalSessionTransitionException.class, () -> cooking.resume(T0));
  }

  @Test
  void aRecipeWithoutStepsFinishesRightAway() {
    Recipe noSteps =
        new Recipe(
            RecipeId.newId(),
            "Salad",
            1,
            5,
            RecipeSource.MANUAL,
            List.of(RecipeRequirement.of(RICE, Grams.of(10))),
            List.of());
    CookingSession session =
        CookingSession.start(HouseholdId.newId(), noSteps, 1, UserId.newId(), T0);

    session.next(T0);

    assertEquals(new Finished(0, T0), session.state());
    assertTrue(session.completions().isEmpty());
  }

  @Test
  void restoresAndValidatesState() {
    CookingSession original = start();
    original.next(T0);
    CookingSession restored =
        CookingSession.restore(
            original.id(),
            original.household(),
            original.recipe(),
            original.startedBy(),
            original.startedAt(),
            new Paused(3, T0),
            T0,
            List.of(new StepCompletion(1, T0), new StepCompletion(2, T0)),
            DeviceId.newId());

    assertTrue(restored.scale().isPresent());
    assertTrue(original.scale().isEmpty());
    DeviceId scale = DeviceId.newId();
    original.useScale(scale);
    assertEquals(scale, original.scale().orElseThrow());
    original.abandon(T0);
    assertThrows(IllegalSessionTransitionException.class, () -> original.useScale(scale));
    restored.resume(T0.plusSeconds(1));
    assertEquals(3, restored.currentStep());
    assertEquals(2, restored.completions().size());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            CookingSession.restore(
                original.id(),
                original.household(),
                original.recipe(),
                original.startedBy(),
                T0,
                new Cooking(4),
                T0,
                List.of(),
                null));
    assertThrows(IllegalArgumentException.class, () -> new Cooking(0));
    assertThrows(IllegalArgumentException.class, () -> new Paused(0, T0));
    assertThrows(IllegalArgumentException.class, () -> new StepCompletion(0, T0));
    assertTrue(SessionPhase.PAUSED.active());
  }
}
