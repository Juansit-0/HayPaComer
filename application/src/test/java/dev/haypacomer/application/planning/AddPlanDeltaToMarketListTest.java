package dev.haypacomer.application.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
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
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.planning.Meal;
import dev.haypacomer.domain.planning.PlanEntry;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddPlanDeltaToMarketListTest {

  private static final Instant NOW = Instant.parse("2026-10-14T15:00:00Z");
  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
  private static final FoodMetadata RICE = food("Rice");
  private static final FoodMetadata CHICKEN = food("Chicken breast");
  private static final FoodMetadata ONION = food("Onion");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final InMemoryPlanningStores planning = new InMemoryPlanningStores();
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
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final UserId guest = UserId.newId();
  private Household household;
  private Fridge fridge;
  private AddPlanDeltaToMarketList delta;

  private static FoodMetadata food(String name) {
    return new FoodMetadata(
        name, FoodCategory.OTHER, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static Recipe recipe(String name, RecipeRequirement... requirements) {
    return new Recipe(
        RecipeId.newId(), name, 2, 30, RecipeSource.MANUAL, List.of(requirements), List.of());
  }

  private FoodItem put(FoodMetadata food, long grams, LocalDate expiry) {
    FoodItem item = new FoodItem(FoodItemId.newId(), food, Grams.of(grams), Grams.ZERO, expiry);
    fridge.place(item, fridge.trays().findFirst().orElseThrow().id());
    return item;
  }

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    Recipe riceBowl = recipe("Rice bowl", RecipeRequirement.of(RICE, Grams.of(100)));
    Recipe chickenRice =
        recipe(
            "Rice with chicken",
            RecipeRequirement.of(RICE, Grams.of(100)),
            RecipeRequirement.of(CHICKEN, Grams.of(200)),
            RecipeRequirement.optional(ONION, Grams.of(75)));
    planning.plans.save(
        WeeklyPlan.create(
            household.id(),
            MONDAY,
            List.of(
                PlanEntry.of(1, Meal.LUNCH, chickenRice, 2, true),
                PlanEntry.of(3, Meal.LUNCH, riceBowl, 2, false),
                PlanEntry.of(3, Meal.DINNER, chickenRice, 4, true),
                PlanEntry.of(5, Meal.LUNCH, riceBowl, 4, false))));
    delta =
        new AddPlanDeltaToMarketList(
            households,
            planning.plans,
            new ViewInventory(
                households,
                fridges,
                stores.ownerships,
                new InMemoryColdChainRepository(),
                FreshnessPolicy.DEFAULT),
            lists,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void addsTheWeekTotalMinusWhatIsAtHomeAndPendingOnce() {
    put(RICE, 300, null);
    put(RICE, 500, LocalDate.of(2026, 10, 1));
    FoodItem privateChicken = put(CHICKEN, 400, null);
    stores.ownerships.save(
        privateChicken.id(),
        Ownership.of(household.membershipOf(juan).orElseThrow().member(), Visibility.PRIVATE));

    List<PlanDeltaItem> forAna = delta.add(ana, household.id());

    assertEquals(List.of(RICE, CHICKEN), forAna.stream().map(PlanDeltaItem::food).toList());
    PlanDeltaItem rice = forAna.getFirst();
    assertEquals(Grams.of(500), rice.needed());
    assertEquals(Grams.of(300), rice.available());
    assertEquals(Grams.of(200), rice.added());
    assertEquals(Grams.of(400), forAna.get(1).needed());
    assertEquals(Grams.of(400), forAna.get(1).added());
    MarketList list = saved.get(household.id());
    assertEquals(2, list.pending().size());
    assertTrue(list.pending().stream().allMatch(item -> item.source() == MarketSource.PLAN));
    assertEquals(Grams.ZERO, list.pendingGrams(ONION));

    List<PlanDeltaItem> again = delta.add(ana, household.id());
    assertTrue(again.stream().allMatch(item -> item.added().isZero()));
    assertEquals(Grams.of(200), saved.get(household.id()).pendingGrams(RICE));

    List<PlanDeltaItem> forJuan = delta.add(juan, household.id());
    assertEquals(List.of(RICE), forJuan.stream().map(PlanDeltaItem::food).toList());
  }

  @Test
  void needsACurrentPlanAndMarketPermission() {
    assertThrows(AccessDeniedException.class, () -> delta.add(guest, household.id()));
    planning.planList.clear();
    assertThrows(WeeklyPlanNotFoundException.class, () -> delta.add(juan, household.id()));
    assertTrue(saved.isEmpty());
  }

  @Test
  void nothingMissingLeavesTheListUntouched() {
    put(RICE, 1_000, null);
    put(CHICKEN, 1_000, null);

    assertTrue(delta.add(juan, household.id()).isEmpty());
    assertTrue(saved.isEmpty());
  }
}
