package dev.haypacomer.application.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryPlanningStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.planning.Meal;
import dev.haypacomer.domain.planning.PlanEntry;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.planning.WeeklyPlanId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.ClonedRecipe;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloneUseCasesTest {

  private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");
  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
  private static final FoodMetadata RICE =
      new FoodMetadata(
          "Rice", FoodCategory.GRAIN, Unit.GRAM, ConversionFactors.MASS_ONLY, false, 365, Set.of());

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryPlanningStores planning = new InMemoryPlanningStores();
  private final UserId juan = UserId.newId();
  private final UserId guest = UserId.newId();
  private final Recipe template = recipe("Rice bowl", RecipeSource.TEMPLATE);
  private final Recipe own = recipe("Plain rice", RecipeSource.MANUAL);
  private Household household;
  private Household neighbours;

  private static Recipe recipe(String name, RecipeSource source) {
    return new Recipe(
        RecipeId.newId(),
        name,
        2,
        20,
        source,
        List.of(RecipeRequirement.of(RICE, Grams.of(150))),
        List.of());
  }

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
    neighbours =
        Household.create("Next door", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    households.save(neighbours);
    planning.templateList.add(template);
    planning.recipes.save(household.id(), own);
  }

  @Test
  void clonesTemplatesAndOwnRecipesIntoTheHousehold() {
    CloneRecipe clone = new CloneRecipe(households, planning.recipes, planning.templates);

    ClonedRecipe fromTemplate = clone.clone(juan, household.id(), template.id());
    ClonedRecipe fromOwn = clone.clone(juan, household.id(), own.id());

    assertEquals(template.id(), planning.clonedFrom.get(fromTemplate.recipe().id()));
    assertEquals(RecipeSource.MANUAL, fromTemplate.recipe().source());
    assertEquals(own.id(), fromOwn.clonedFrom());
    assertEquals(3, planning.recipes.findByHousehold(household.id()).size());
    assertEquals(
        List.of("Rice bowl"),
        new ListRecipeTemplates(planning.templates).list().stream().map(Recipe::name).toList());
    assertThrows(RecipeNotFoundException.class, () -> clone.clone(juan, neighbours.id(), own.id()));
    assertThrows(
        RecipeNotFoundException.class, () -> clone.clone(juan, household.id(), RecipeId.newId()));
    assertThrows(
        AccessDeniedException.class, () -> clone.clone(guest, household.id(), template.id()));
  }

  @Test
  void clonesAPlanToAnotherWeekOfTheSameHousehold() {
    WeeklyPlan original =
        WeeklyPlan.create(
            household.id(), MONDAY, List.of(PlanEntry.of(1, Meal.LUNCH, own, 2, false)));
    planning.plans.save(original);
    CloneWeeklyPlan clone = new CloneWeeklyPlan(households, planning.plans);

    WeeklyPlan copy = clone.clone(juan, household.id(), original.id(), MONDAY.plusWeeks(1));

    assertNotEquals(original.id(), copy.id());
    assertEquals(original.id(), copy.clonedFrom().orElseThrow());
    assertEquals(2, planning.planList.size());
    assertEquals(
        copy.id(),
        planning.plans.current(household.id(), MONDAY.plusWeeks(1).plusDays(2)).orElseThrow().id());
    assertThrows(
        WeeklyPlanNotFoundException.class,
        () -> clone.clone(juan, neighbours.id(), original.id(), MONDAY.plusWeeks(2)));
    assertThrows(
        WeeklyPlanNotFoundException.class,
        () -> clone.clone(juan, household.id(), WeeklyPlanId.newId(), MONDAY.plusWeeks(2)));
    assertThrows(
        AccessDeniedException.class,
        () -> clone.clone(guest, household.id(), original.id(), MONDAY.plusWeeks(2)));
    assertThrows(
        IllegalArgumentException.class,
        () -> clone.clone(juan, household.id(), original.id(), MONDAY));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> clone.clone(UserId.newId(), household.id(), original.id(), MONDAY.plusWeeks(3)));
  }
}
