package dev.haypacomer.domain.recipe;

import static dev.haypacomer.domain.recipe.RecipeTest.CHICKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RecipeStepTest {

  @Test
  void plainStepHasNoTimerOrWeighing() {
    RecipeStep step = RecipeStep.of(1, "  Chop onions ");

    assertEquals("Chop onions", step.instruction());
    assertTrue(step.timerDuration().isEmpty());
    assertTrue(step.weighingTarget().isEmpty());
    assertSame(step, step.scaledBy(new BigDecimal("2")));
  }

  @Test
  void rejectsInvalidStepData() {
    assertThrows(IllegalArgumentException.class, () -> RecipeStep.of(0, "Cook"));
    assertThrows(IllegalArgumentException.class, () -> RecipeStep.of(1, " "));
    assertThrows(NullPointerException.class, () -> RecipeStep.of(1, null));
    assertThrows(
        IllegalArgumentException.class, () -> RecipeStep.of(1, "Wait").withTimer(Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> RecipeStep.of(1, "Wait").withTimer(Duration.ofSeconds(-5)));
  }

  @Test
  void rejectsZeroTargetsAndRequirements() {
    assertThrows(IllegalArgumentException.class, () -> new StepWeighing(CHICKEN, Grams.ZERO));
    assertThrows(IllegalArgumentException.class, () -> RecipeRequirement.of(CHICKEN, Grams.ZERO));
    assertThrows(NullPointerException.class, () -> RecipeRequirement.of(null, Grams.of(1)));
    assertThrows(NullPointerException.class, () -> new StepWeighing(CHICKEN, null));
    assertThrows(NullPointerException.class, () -> new RecipeId(null));
  }
}
