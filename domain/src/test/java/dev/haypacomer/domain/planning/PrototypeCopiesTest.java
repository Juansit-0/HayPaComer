package dev.haypacomer.domain.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.ClonedRecipe;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PrototypeCopiesTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
  private static final FoodMetadata RICE =
      new FoodMetadata(
          "Rice", FoodCategory.GRAIN, Unit.GRAM, ConversionFactors.MASS_ONLY, false, 365, Set.of());
  private static final Recipe TEMPLATE =
      new Recipe(
          RecipeId.newId(),
          "Rice bowl",
          2,
          20,
          RecipeSource.TEMPLATE,
          List.of(RecipeRequirement.of(RICE, Grams.of(150))),
          List.of(RecipeStep.of(1, "Boil")));
  private static final Recipe PLAIN =
      new Recipe(
          RecipeId.newId(),
          "Plain rice",
          1,
          15,
          RecipeSource.MANUAL,
          List.of(RecipeRequirement.of(RICE, Grams.of(80))),
          List.of());

  @Test
  void aRecipeCopyHasItsOwnIdAndRemembersItsOrigin() {
    ClonedRecipe copy = ClonedRecipe.of(TEMPLATE);

    assertNotEquals(TEMPLATE.id(), copy.recipe().id());
    assertEquals(TEMPLATE.id(), copy.clonedFrom());
    assertEquals(RecipeSource.MANUAL, copy.recipe().source());
    assertEquals(TEMPLATE.requirements(), copy.recipe().requirements());
    assertEquals(TEMPLATE.steps(), copy.recipe().steps());
    assertEquals(RecipeSource.MANUAL, PLAIN.copy().source());
    assertThrows(IllegalArgumentException.class, () -> new ClonedRecipe(PLAIN, PLAIN.id()));
  }

  @Test
  void aPlanCopyIsIndependentFromTheOriginal() {
    WeeklyPlan original =
        WeeklyPlan.create(
            HouseholdId.newId(),
            MONDAY,
            List.of(
                PlanEntry.of(1, Meal.LUNCH, TEMPLATE, 2, false),
                PlanEntry.of(2, Meal.DINNER, PLAIN, 1, true)));

    WeeklyPlan copy = original.cloneFor(MONDAY.plusWeeks(1));
    PlanEntry copiedLunch = copy.entries().getFirst();
    copy.change(copiedLunch.id(), PLAIN, 4, true);

    assertNotEquals(original.id(), copy.id());
    assertEquals(original.id(), copy.clonedFrom().orElseThrow());
    assertTrue(original.clonedFrom().isEmpty());
    assertEquals(MONDAY.plusWeeks(1), copy.weekStart());
    assertEquals(original.household(), copy.household());
    assertEquals(2, copy.entries().size());
    assertNotEquals(original.entries().getFirst().id(), copiedLunch.id());
    assertEquals(TEMPLATE, original.entries().getFirst().recipe());
    assertEquals(2, original.entries().getFirst().servings());
    assertEquals(PLAIN, copy.entries().getFirst().recipe());
    assertTrue(copy.entries().get(1).needsShopping());
    assertThrows(IllegalArgumentException.class, () -> original.cloneFor(MONDAY));
  }
}
