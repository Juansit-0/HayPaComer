package dev.haypacomer.application.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryCookingStores;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.application.support.InMemoryPlanningStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.planning.Meal;
import dev.haypacomer.domain.planning.PlanEntry;
import dev.haypacomer.domain.planning.PlanEntryId;
import dev.haypacomer.domain.planning.RescueFirstStrategy;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeeklyPlanUseCasesTest {

  private static final Instant NOW = Instant.parse("2026-10-12T15:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);
  private static final FoodMetadata LENTILS = food("Lentils", FoodCategory.LEGUME);

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final InMemoryCookingStores cooking = new InMemoryCookingStores();
  private final InMemoryPlanningStores planning = new InMemoryPlanningStores();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId guest = UserId.newId();
  private final Recipe chickenRice =
      recipe(
          "Rice with chicken",
          RecipeRequirement.of(CHICKEN, Grams.of(200)),
          RecipeRequirement.of(RICE, Grams.of(150)));
  private final Recipe lentilStew =
      recipe("Lentil stew", RecipeRequirement.of(LENTILS, Grams.of(200)));
  private Household household;
  private ViewInventory inventory;

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static Recipe recipe(String name, RecipeRequirement... requirements) {
    return new Recipe(
        RecipeId.newId(), name, 2, 30, RecipeSource.MANUAL, List.of(requirements), List.of());
  }

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
    Fridge fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    fridge.place(
        new FoodItem(FoodItemId.newId(), CHICKEN, Grams.of(200), Grams.ZERO, TODAY.plusDays(1)),
        fridge.trays().findFirst().orElseThrow().id());
    fridge.place(
        new FoodItem(FoodItemId.newId(), RICE, Grams.of(1_000), Grams.ZERO, null),
        fridge.trays().findFirst().orElseThrow().id());
    inventory =
        new ViewInventory(
            households,
            fridges,
            stores.ownerships,
            new InMemoryColdChainRepository(),
            FreshnessPolicy.DEFAULT);
    SaveRecipe save = new SaveRecipe(households, planning.recipes);
    save.save(juan, household.id(), lentilStew);
    save.save(juan, household.id(), chickenRice);
  }

  private GenerateWeeklyPlan generate() {
    return new GenerateWeeklyPlan(
        households,
        inventory,
        cooking.profiles,
        planning.recipes,
        planning.plans,
        new RescueFirstStrategy(),
        clock);
  }

  @Test
  void generatesARescueFirstWeekStartingToday() {
    WeeklyPlan plan = generate().generate(juan, household.id(), 2, Set.of());

    assertEquals(TODAY, plan.weekStart());
    assertEquals(14, plan.entries().size());
    assertEquals(chickenRice, plan.entries().getFirst().recipe());
    assertFalse(plan.entries().getFirst().needsShopping());
    assertTrue(plan.entries().get(1).needsShopping());
    assertEquals(
        plan.id(),
        new ViewCurrentPlan(households, planning.plans, clock).view(guest, household.id()).id());
    assertEquals(
        List.of("Lentil stew", "Rice with chicken"),
        new ListRecipes(households, planning.recipes)
            .list(guest, household.id()).stream().map(Recipe::name).toList());

    WeeklyPlan again = generate().generate(juan, household.id(), 3, Set.of());
    assertEquals(1, planning.planList.size());
    assertEquals(3, again.entries().getFirst().servings());
  }

  @Test
  void changesAnEntryAndRechecksTheFridge() {
    WeeklyPlan plan = generate().generate(juan, household.id(), 2, Set.of());
    PlanEntry dinner =
        plan.entries().stream()
            .filter(entry -> entry.day() == 1 && entry.meal() == Meal.DINNER)
            .findFirst()
            .orElseThrow();
    ChangePlanEntry change =
        new ChangePlanEntry(
            households,
            planning.recipes,
            planning.plans,
            new EvaluateRecipe(households, inventory, cooking.profiles, cooking.rules, clock));

    PlanEntry changed = change.change(juan, household.id(), dinner.id(), chickenRice.id(), 2);

    assertEquals(chickenRice, changed.recipe());
    assertFalse(changed.needsShopping());
    assertTrue(
        change.change(juan, household.id(), dinner.id(), lentilStew.id(), 2).needsShopping());
    assertThrows(
        WeeklyPlanNotFoundException.class,
        () -> change.change(juan, household.id(), PlanEntryId.newId(), lentilStew.id(), 2));
    assertThrows(
        RecipeNotFoundException.class,
        () -> change.change(juan, household.id(), dinner.id(), RecipeId.newId(), 2));
    assertThrows(
        AccessDeniedException.class,
        () -> change.change(guest, household.id(), dinner.id(), lentilStew.id(), 2));
  }

  @Test
  void guardsAccessAndMissingPlans() {
    assertThrows(
        WeeklyPlanNotFoundException.class,
        () -> new ViewCurrentPlan(households, planning.plans, clock).view(juan, household.id()));
    assertThrows(
        AccessDeniedException.class, () -> generate().generate(guest, household.id(), 2, Set.of()));
    assertThrows(
        AccessDeniedException.class,
        () -> new SaveRecipe(households, planning.recipes).save(guest, household.id(), lentilStew));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> new ListRecipes(households, planning.recipes).list(UserId.newId(), household.id()));
    assertThrows(
        IllegalArgumentException.class,
        () -> generate().generate(juan, household.id(), 2, Set.of(MemberId.newId())));
    MemberId juanMember = household.membershipOf(juan).orElseThrow().member();
    assertEquals(
        14, generate().generate(juan, household.id(), 1, Set.of(juanMember)).entries().size());
  }
}
