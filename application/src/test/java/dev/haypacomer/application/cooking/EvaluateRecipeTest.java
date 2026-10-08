package dev.haypacomer.application.cooking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RequirementVerdict;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvaluateRecipeTest {

  private static final Instant NOW = Instant.parse("2026-10-08T17:00:00Z");

  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata TUNA = food("Tuna", FoodCategory.FISH);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household household;
  private Fridge fridge;
  private Tray top;
  private EvaluateRecipe evaluate;

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static final Recipe RICE_WITH_CHICKEN =
      new Recipe(
          RecipeId.newId(),
          "Rice with chicken",
          2,
          35,
          RecipeSource.MANUAL,
          List.of(
              RecipeRequirement.of(CHICKEN, Grams.of(200)),
              RecipeRequirement.of(RICE, Grams.of(150))),
          List.of());

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    top = fridge.trays().findFirst().orElseThrow();
    evaluate =
        new EvaluateRecipe(
            households,
            new ViewInventory(
                households,
                fridges,
                stores.ownerships,
                new InMemoryColdChainRepository(),
                FreshnessPolicy.DEFAULT),
            clock);
  }

  private FoodItem put(FoodMetadata food, long grams, LocalDate expiry) {
    FoodItem item = new FoodItem(FoodItemId.newId(), food, Grams.of(grams), Grams.ZERO, expiry);
    fridge.place(item, top.id());
    return item;
  }

  @Test
  void theDemoCaseReducesToOnePortionOrSubstitutesWithTuna() {
    put(CHICKEN, 80, null);
    put(RICE, 300, null);
    put(TUNA, 130, null);
    put(TUNA, 130, null);

    RecipeEvaluation flexible =
        evaluate.evaluate(
            ana, household.id(), RICE_WITH_CHICKEN, 2, StrategyKind.FLEXIBLE, f -> List.of());
    RecipeEvaluation rescue =
        evaluate.evaluate(
            ana,
            household.id(),
            RICE_WITH_CHICKEN,
            2,
            StrategyKind.RESCUE,
            Map.of(CHICKEN, List.of(TUNA))::get);
    RecipeEvaluation strict =
        evaluate.evaluate(
            ana, household.id(), RICE_WITH_CHICKEN, 2, StrategyKind.STRICT, f -> List.of());

    assertEquals(RequirementVerdict.REDUCE, flexible.verdict());
    assertEquals(1, flexible.achievableServings());
    assertEquals(RequirementVerdict.SUBSTITUTE, rescue.verdict());
    assertEquals(RequirementVerdict.MISSING, strict.verdict());
    assertEquals(Grams.of(120), strict.missing().getFirst().shortfall());
  }

  @Test
  void ignoresExpiredFoodAndFoodTheViewerCannotUse() {
    put(CHICKEN, 300, LocalDate.of(2026, 10, 1));
    FoodItem privateChicken = put(CHICKEN, 300, null);
    stores.ownerships.save(
        privateChicken.id(),
        Ownership.of(household.membershipOf(juan).orElseThrow().member(), Visibility.PRIVATE));
    put(RICE, 300, null);

    assertEquals(
        RequirementVerdict.MISSING,
        evaluate
            .evaluate(
                ana, household.id(), RICE_WITH_CHICKEN, 2, StrategyKind.STRICT, f -> List.of())
            .verdict());
    assertEquals(
        RequirementVerdict.ENOUGH,
        evaluate
            .evaluate(
                juan, household.id(), RICE_WITH_CHICKEN, 2, StrategyKind.STRICT, f -> List.of())
            .verdict());
  }
}
