package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.CHICKEN;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.EGG;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.planning.Meal;
import dev.haypacomer.domain.planning.PlanEntry;
import dev.haypacomer.domain.planning.PlanEntryId;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostgresPlanningRepositoriesTest extends PostgresTestSupport {

  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

  private static final Recipe OMELETTE =
      new Recipe(
          RecipeId.newId(),
          "Chicken omelette",
          1,
          15,
          RecipeSource.MANUAL,
          List.of(
              RecipeRequirement.of(EGG, Grams.of(100)),
              RecipeRequirement.optional(CHICKEN, Grams.of(50))),
          List.of(
              RecipeStep.of(1, "Weigh the chicken")
                  .withWeighing(new StepWeighing(CHICKEN, Grams.of(50))),
              RecipeStep.of(2, "Cook").withTimer(Duration.ofMinutes(4))));

  private static final Recipe BOILED_EGG =
      new Recipe(
          RecipeId.newId(),
          "Boiled egg",
          1,
          10,
          RecipeSource.MANUAL,
          List.of(RecipeRequirement.of(EGG, Grams.of(50))),
          List.of());

  private Household household() {
    PostgresFoodCatalogRepository catalog = new PostgresFoodCatalogRepository(dataSource);
    catalog.save(EGG);
    catalog.save(CHICKEN);
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    return household;
  }

  @Test
  void storesRecipesWithRequirementsAndSteps() {
    Household household = household();
    PostgresRecipeRepository recipes = new PostgresRecipeRepository(dataSource);

    recipes.save(household.id(), OMELETTE);
    recipes.save(household.id(), BOILED_EGG);
    recipes.save(household.id(), OMELETTE);

    assertEquals(OMELETTE, recipes.find(household.id(), OMELETTE.id()).orElseThrow());
    assertEquals(List.of(OMELETTE, BOILED_EGG), recipes.findByHousehold(household.id()));
    assertTrue(recipes.find(HouseholdId.newId(), OMELETTE.id()).isEmpty());
    assertTrue(recipes.findByHousehold(HouseholdId.newId()).isEmpty());
  }

  @Test
  void keepsOnePlanPerWeekAndFindsItByDateOrEntry() {
    Household household = household();
    PostgresRecipeRepository recipes = new PostgresRecipeRepository(dataSource);
    recipes.save(household.id(), OMELETTE);
    recipes.save(household.id(), BOILED_EGG);
    PostgresWeeklyPlanRepository plans = new PostgresWeeklyPlanRepository(dataSource);
    PlanEntry lunch = PlanEntry.of(1, Meal.LUNCH, OMELETTE, 2, false);
    WeeklyPlan first =
        WeeklyPlan.create(
            household.id(),
            MONDAY,
            List.of(lunch, PlanEntry.of(1, Meal.DINNER, BOILED_EGG, 2, true)));
    plans.save(first);

    WeeklyPlan loaded = plans.current(household.id(), MONDAY.plusDays(6)).orElseThrow();
    assertEquals(first.id(), loaded.id());
    assertEquals(first.entries(), loaded.entries());
    assertTrue(plans.current(household.id(), MONDAY.plusDays(7)).isEmpty());
    assertTrue(plans.current(household.id(), MONDAY.minusDays(1)).isEmpty());
    assertEquals(first.id(), plans.findByEntry(household.id(), lunch.id()).orElseThrow().id());
    assertTrue(plans.findByEntry(household.id(), PlanEntryId.newId()).isEmpty());

    loaded.change(lunch.id(), BOILED_EGG, 3, true);
    plans.save(loaded);
    assertEquals(
        BOILED_EGG,
        plans
            .current(household.id(), MONDAY)
            .orElseThrow()
            .entry(lunch.id())
            .orElseThrow()
            .recipe());

    WeeklyPlan replacement =
        WeeklyPlan.create(
            household.id(), MONDAY, List.of(PlanEntry.of(2, Meal.LUNCH, OMELETTE, 1, false)));
    plans.save(replacement);
    assertEquals(replacement.id(), plans.current(household.id(), MONDAY).orElseThrow().id());
    assertTrue(plans.findByEntry(household.id(), lunch.id()).isEmpty());
  }
}
