package dev.haypacomer.domain.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WeeklyPlanTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);
  private static final FoodMetadata LENTILS = food("Lentils", FoodCategory.LEGUME);
  private static final FoodMetadata EGG =
      new FoodMetadata(
          "Egg",
          FoodCategory.EGGS,
          Unit.PIECE,
          ConversionFactors.withPieceWeight("50"),
          true,
          21,
          Set.of(Allergen.EGGS));

  private static final Recipe CHICKEN_RICE =
      recipe(
          "Rice with chicken",
          RecipeRequirement.of(CHICKEN, Grams.of(200)),
          RecipeRequirement.of(RICE, Grams.of(150)));
  private static final Recipe LENTIL_STEW =
      recipe("Lentil stew", RecipeRequirement.of(LENTILS, Grams.of(200)));
  private static final Recipe OMELETTE =
      recipe("Omelette", RecipeRequirement.of(EGG, Grams.of(150)));
  private static final Recipe FRIED_RICE =
      recipe("Fried rice", RecipeRequirement.of(RICE, Grams.of(200)));

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static Recipe recipe(String name, RecipeRequirement... requirements) {
    return new Recipe(
        RecipeId.newId(), name, 2, 30, RecipeSource.MANUAL, List.of(requirements), List.of());
  }

  private static PlanningContext context(DiningGroup diners, List<Recipe> recipes) {
    return new PlanningContext(
        recipes,
        new Availability()
            .add(CHICKEN, Grams.of(200))
            .add(RICE, Grams.of(1_000))
            .add(LENTILS, Grams.of(200))
            .add(EGG, Grams.of(300)),
        Set.of(CHICKEN.key()),
        diners,
        2);
  }

  @Test
  void rescuesExpiringFoodFirstAndNeverRepeatsOnConsecutiveDays() {
    List<PlanEntry> entries =
        new RescueFirstStrategy()
            .plan(
                context(
                    DiningGroup.of(), List.of(FRIED_RICE, LENTIL_STEW, OMELETTE, CHICKEN_RICE)));

    assertEquals(14, entries.size());
    assertEquals(CHICKEN_RICE, entries.getFirst().recipe());
    assertEquals(1, entries.getFirst().day());
    assertEquals(Meal.LUNCH, entries.getFirst().meal());
    assertFalse(entries.getFirst().needsShopping());
    for (int index = 2; index < entries.size(); index++) {
      PlanEntry entry = entries.get(index);
      PlanEntry sameMealYesterday = entries.get(index - 2);
      assertNotEquals(sameMealYesterday.recipe(), entry.recipe());
    }
    assertTrue(
        entries.stream()
            .filter(entry -> entry.recipe().equals(CHICKEN_RICE))
            .skip(1)
            .allMatch(PlanEntry::needsShopping));
  }

  @Test
  void skipsRecipesThatADinerCannotEat() {
    DiningGroup eggAllergy =
        DiningGroup.of(
            new FoodProfile(MemberId.newId(), Diet.OMNIVORE, Set.of(Allergen.EGGS), Set.of()));

    List<PlanEntry> entries =
        new RescueFirstStrategy()
            .plan(context(eggAllergy, List.of(OMELETTE, LENTIL_STEW, FRIED_RICE)));

    assertTrue(entries.stream().noneMatch(entry -> entry.recipe().equals(OMELETTE)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RescueFirstStrategy().plan(context(eggAllergy, List.of(OMELETTE))));
  }

  @Test
  void aSingleRecipeStillFillsTheWeek() {
    List<PlanEntry> entries =
        new RescueFirstStrategy().plan(context(DiningGroup.of(), List.of(LENTIL_STEW)));

    assertEquals(14, entries.size());
    assertFalse(entries.getFirst().needsShopping());
    assertTrue(entries.get(1).needsShopping());
  }

  @Test
  void thePlanKeepsOneEntryPerSlotAndCanChangeOne() {
    WeeklyPlan plan =
        WeeklyPlan.create(
            HouseholdId.newId(),
            MONDAY,
            new RescueFirstStrategy()
                .plan(context(DiningGroup.of(), List.of(LENTIL_STEW, FRIED_RICE))));
    PlanEntry first = plan.entries().getFirst();

    PlanEntry changed = plan.change(first.id(), OMELETTE, 4, true);

    assertEquals(OMELETTE, plan.entry(first.id()).orElseThrow().recipe());
    assertEquals(4, changed.servings());
    assertEquals(MONDAY.plusDays(6), plan.weekEnd());
    assertEquals(MONDAY, plan.dateOf(first));
    assertTrue(plan.covers(MONDAY.plusDays(3)));
    assertFalse(plan.covers(MONDAY.plusDays(7)));
    assertFalse(plan.covers(MONDAY.minusDays(1)));
    assertThrows(
        IllegalArgumentException.class, () -> plan.change(PlanEntryId.newId(), OMELETTE, 2, false));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WeeklyPlan.create(
                HouseholdId.newId(),
                MONDAY,
                List.of(
                    PlanEntry.of(1, Meal.LUNCH, OMELETTE, 2, false),
                    PlanEntry.of(1, Meal.LUNCH, LENTIL_STEW, 2, false))));
    assertThrows(
        IllegalArgumentException.class, () -> PlanEntry.of(8, Meal.LUNCH, OMELETTE, 2, false));
    assertThrows(
        IllegalArgumentException.class, () -> PlanEntry.of(1, Meal.DINNER, OMELETTE, 0, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PlanningContext(List.of(), new Availability(), Set.of(), DiningGroup.of(), 0));
  }
}
