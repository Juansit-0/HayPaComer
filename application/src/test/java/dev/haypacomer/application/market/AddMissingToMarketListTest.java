package dev.haypacomer.application.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryCookingStores;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddMissingToMarketListTest {

  private static final Instant NOW = Instant.parse("2026-10-08T17:00:00Z");
  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);
  private static final FoodMetadata SOY = food("Soy sauce", FoodCategory.CONDIMENT);
  private static final Recipe RICE_WITH_CHICKEN =
      new Recipe(
          RecipeId.newId(),
          "Rice with chicken",
          2,
          35,
          RecipeSource.MANUAL,
          List.of(
              RecipeRequirement.of(CHICKEN, Grams.of(200)),
              RecipeRequirement.of(RICE, Grams.of(150)),
              RecipeRequirement.optional(SOY, Grams.of(15))),
          List.of());

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final InMemoryCookingStores cooking = new InMemoryCookingStores();
  private final Map<HouseholdId, MarketList> saved = new HashMap<>();
  private final MarketListRepository lists =
      new MarketListRepository() {
        @Override
        public void save(MarketList list) {
          saved.put(list.household(), MarketList.restore(list.household(), list.items()));
        }

        @Override
        public Optional<MarketList> findByHousehold(HouseholdId household) {
          return Optional.ofNullable(saved.get(household))
              .map(list -> MarketList.restore(household, list.items()));
        }
      };
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId guest = UserId.newId();
  private Household household;
  private Fridge fridge;
  private AddMissingToMarketList addMissing;

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    EvaluateRecipe evaluate =
        new EvaluateRecipe(
            households,
            new ViewInventory(
                households,
                fridges,
                stores.ownerships,
                new InMemoryColdChainRepository(),
                FreshnessPolicy.DEFAULT),
            cooking.profiles,
            cooking.rules,
            clock);
    addMissing = new AddMissingToMarketList(households, evaluate, lists, clock);
  }

  private void put(FoodMetadata food, long grams) {
    fridge.place(
        new FoodItem(FoodItemId.newId(), food, Grams.of(grams), Grams.ZERO, null),
        fridge.trays().findFirst().orElseThrow().id());
  }

  @Test
  void addsTheMandatoryShortfallsOnce() {
    put(CHICKEN, 80);
    put(RICE, 100);

    List<MissingItem> first = addMissing.add(juan, household.id(), RICE_WITH_CHICKEN, 2);

    assertEquals(2, first.size());
    assertEquals(Grams.of(120), first.getFirst().shortfall());
    assertEquals(Grams.of(120), first.getFirst().added());
    assertEquals(Grams.of(50), first.get(1).added());
    MarketList list = saved.get(household.id());
    assertEquals(2, list.pending().size());
    assertEquals(MarketSource.RECIPE, list.pending().getFirst().source());

    List<MissingItem> again = addMissing.add(juan, household.id(), RICE_WITH_CHICKEN, 2);
    assertEquals(Grams.ZERO, again.getFirst().added());
    assertEquals(Grams.of(120), again.getFirst().pending());

    List<MissingItem> moreServings = addMissing.add(juan, household.id(), RICE_WITH_CHICKEN, 4);
    assertEquals(Grams.of(320), moreServings.getFirst().pending());
    assertEquals(Grams.of(200), moreServings.getFirst().added());
  }

  @Test
  void nothingMissingLeavesTheListAlone() {
    put(CHICKEN, 500);
    put(RICE, 500);

    assertTrue(addMissing.add(juan, household.id(), RICE_WITH_CHICKEN, 2).isEmpty());
    assertTrue(saved.isEmpty());
  }

  @Test
  void guestsCannotChangeTheMarketList() {
    assertThrows(
        AccessDeniedException.class,
        () -> addMissing.add(guest, household.id(), RICE_WITH_CHICKEN, 2));
  }
}
