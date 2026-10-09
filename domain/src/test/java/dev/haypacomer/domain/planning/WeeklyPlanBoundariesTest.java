package dev.haypacomer.domain.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.cooking.Availability;
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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WeeklyPlanBoundariesTest {

  private static final FoodMetadata CHICKEN = food("Chicken breast");
  private static final FoodMetadata RICE = food("Rice");
  private static final FoodMetadata BEANS = food("Beans");

  private static final Recipe CHICKEN_RICE =
      recipe(
          "Rice with chicken",
          RecipeRequirement.of(CHICKEN, Grams.of(200)),
          RecipeRequirement.of(RICE, Grams.of(150)));
  private static final Recipe BEAN_BOWL =
      recipe("Bean bowl", RecipeRequirement.of(BEANS, Grams.of(200)));
  private static final Recipe ARROZ =
      recipe("Arroz blanco", RecipeRequirement.of(RICE, Grams.of(100)));

  private static FoodMetadata food(String name) {
    return new FoodMetadata(
        name, FoodCategory.OTHER, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static Recipe recipe(String name, RecipeRequirement... requirements) {
    return new Recipe(
        RecipeId.newId(), name, 2, 30, RecipeSource.MANUAL, List.of(requirements), List.of());
  }

  private static List<PlanEntry> plan(Availability stock, int servings, Recipe... recipes) {
    return new RescueFirstStrategy()
        .plan(
            new PlanningContext(
                List.of(recipes), stock, Set.of(CHICKEN.key()), DiningGroup.of(), servings));
  }

  private static List<String> names(List<PlanEntry> entries) {
    return entries.stream().map(entry -> entry.recipe().name()).toList();
  }

  @Test
  void theSameFridgeAlwaysGivesTheSameWeek() {
    Availability stock = new Availability().add(CHICKEN, Grams.of(200)).add(RICE, Grams.of(800));

    assertEquals(
        names(plan(stock, 2, ARROZ, BEAN_BOWL, CHICKEN_RICE)),
        names(plan(stock, 2, CHICKEN_RICE, BEAN_BOWL, ARROZ)));
  }

  @Test
  void rescueComesFirstEvenWhenThereIsNotEnoughForEveryone() {
    List<PlanEntry> entries =
        plan(
            new Availability().add(CHICKEN, Grams.of(200)).add(RICE, Grams.of(800)),
            4,
            ARROZ,
            CHICKEN_RICE);

    assertEquals(CHICKEN_RICE, entries.getFirst().recipe());
    assertTrue(entries.getFirst().needsShopping());
    assertEquals(4, entries.getFirst().servings());
  }

  @Test
  void rescuePointsStopOnceTheAtRiskFoodIsUsedUp() {
    List<PlanEntry> entries =
        plan(
            new Availability().add(CHICKEN, Grams.of(200)).add(RICE, Grams.of(2_000)),
            2,
            CHICKEN_RICE,
            ARROZ);

    assertEquals(CHICKEN_RICE, entries.getFirst().recipe());
    assertTrue(
        entries.stream()
            .skip(1)
            .noneMatch(entry -> entry.recipe().equals(CHICKEN_RICE) && !entry.needsShopping()));
    assertEquals(ARROZ, entries.get(2).recipe());
  }

  @Test
  void anEmptyFridgeStillPlansEveryMealAndMarksShopping() {
    List<PlanEntry> entries = plan(new Availability(), 2, BEAN_BOWL, ARROZ);

    assertEquals(PlanEntry.DAYS * Meal.values().length, entries.size());
    assertTrue(entries.stream().allMatch(PlanEntry::needsShopping));
    assertEquals("Arroz blanco", entries.getFirst().recipe().name());
  }
}
