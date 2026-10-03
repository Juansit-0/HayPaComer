package dev.haypacomer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.ListFridges;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.ChangeFoodOwnership;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Grant;
import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.application.inventory.ConsumeFoodCommand;
import dev.haypacomer.application.inventory.ExecuteInventoryCommand;
import dev.haypacomer.application.inventory.FoodAccessGuard;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.StockFoodCommand;
import dev.haypacomer.application.inventory.UndoLastChange;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.market.AddToMarketList;
import dev.haypacomer.application.market.ViewMarketList;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.application.support.InMemorySnapshotStore;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FoodStatus;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.member.FoodProfile;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KitchenScenarioTest {

  private static final Instant NOW = Instant.parse("2026-10-03T17:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final InMemorySnapshotStore snapshots = new InMemorySnapshotStore();
  private final Map<HouseholdId, MarketList> lists = new HashMap<>();
  private final MarketListRepository marketLists =
      new MarketListRepository() {
        @Override
        public void save(MarketList list) {
          lists.put(list.household(), MarketList.restore(list.household(), list.items()));
        }

        @Override
        public Optional<MarketList> findByHousehold(HouseholdId household) {
          return Optional.ofNullable(lists.get(household))
              .map(list -> MarketList.restore(household, list.items()));
        }
      };
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();

  private Household household;
  private Fridge fridge;
  private Tray rack;
  private ExecuteInventoryCommand commands;
  private HayPaComerFacade facade;

  private static FoodMetadata food(
      String name, FoodCategory category, Unit unit, Set<Allergen> allergens) {
    return new FoodMetadata(name, category, unit, ConversionFactors.MASS_ONLY, true, 7, allergens);
  }

  @BeforeEach
  void setUpHousehold() {
    household =
        Household.create("Apartment 402", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    households.save(household);
    stores.catalog.save(food("Milk", FoodCategory.DAIRY, Unit.GRAM, Set.of(Allergen.MILK)));
    stores.catalog.save(food("Yogurt", FoodCategory.DAIRY, Unit.GRAM, Set.of(Allergen.MILK)));
    stores.catalog.save(food("Chicken breast", FoodCategory.POULTRY, Unit.GRAM, Set.of()));
    stores.catalog.save(food("Tuna", FoodCategory.FISH, Unit.GRAM, Set.of(Allergen.FISH)));
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    rack = fridge.trays().filter(tray -> tray.name().equals("Rack")).findFirst().orElseThrow();
    commands =
        new ExecuteInventoryCommand(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            stores.catalog,
            new FoodAccessGuard(),
            stores.audit,
            stores.unitOfWork,
            snapshots,
            clock);
    facade =
        new HayPaComerFacade(
            new GetHousehold(households),
            new SetUpFridge(households, fridges),
            new ListFridges(households, fridges),
            new ViewInventory(households, fridges, stores.ownerships, FreshnessPolicy.DEFAULT),
            clock);
  }

  private FoodItemId stock(
      UserId actor, String food, long gross, long tare, LocalDate expiry, Visibility visibility) {
    return commands
        .execute(
            actor,
            new StockFoodCommand(
                UUID.randomUUID(),
                household.id(),
                fridge.id(),
                rack.id(),
                food,
                Grams.of(gross),
                Grams.of(tare),
                expiry,
                visibility))
        .item();
  }

  @Test
  void milkRemovalDiscountsMeasuredGramsWithTare() {
    FoodItemId milk = stock(juan, "Milk", 892, 50, TODAY.plusDays(5), Visibility.SHARED);

    CommandOutcome outcome =
        commands.execute(
            ana,
            new ConsumeFoodCommand(
                UUID.randomUUID(), household.id(), milk, Grams.of(192), MovementSource.SCALE));

    assertEquals(Grams.of(650), outcome.remaining());
    assertEquals(Grams.of(650), facade.snapshot(juan, household.id()).totalGrams());
  }

  @Test
  void anaCannotTakeJuansPrivateYogurtUntilHeGrantsIt() {
    FoodItemId yogurt = stock(juan, "Yogurt", 125, 0, TODAY.plusDays(4), Visibility.PRIVATE);
    ConsumeFoodCommand eat =
        new ConsumeFoodCommand(
            UUID.randomUUID(), household.id(), yogurt, Grams.of(125), MovementSource.MANUAL);

    assertThrows(AccessDeniedException.class, () -> commands.execute(ana, eat));
    InventoryEntry seenByAna = facade.inventory(ana, household.id()).getFirst();
    assertFalse(seenByAna.usable());
    assertTrue(seenByAna.food().has(FoodStatus.PRIVATE));

    new ChangeFoodOwnership(households, fridges, stores.ownerships, stores.movements)
        .change(juan, household.id(), yogurt, new Grant(ana));
    assertEquals(Grams.ZERO, commands.execute(ana, eat).remaining());
  }

  @Test
  void rescueModeSurfacesWhatExpiresFirstAndExcludesExpiredFood() {
    stock(juan, "Chicken breast", 200, 0, TODAY.plusDays(1), Visibility.SHARED);
    stock(juan, "Milk", 892, 50, TODAY.plusDays(6), Visibility.SHARED);
    stock(juan, "Tuna", 130, 0, TODAY.minusDays(1), Visibility.SHARED);

    List<String> rescue =
        facade.rescueFirst(ana, household.id()).stream()
            .map(entry -> entry.food().item().name())
            .toList();

    assertEquals(List.of("Chicken breast"), rescue);
    assertEquals(1, facade.snapshot(ana, household.id()).expired());
  }

  @Test
  void householdListMergesWhatEachMemberAdds() {
    AddToMarketList add = new AddToMarketList(households, marketLists, stores.catalog, clock);

    add.add(juan, household.id(), "Chicken breast", Grams.of(200), MarketSource.MANUAL);
    add.add(ana, household.id(), "chicken breast", Grams.of(120), MarketSource.RECIPE);

    MarketList list = new ViewMarketList(households, marketLists).view(ana, household.id());
    assertEquals(1, list.pending().size());
    assertEquals(Grams.of(320), list.pending().getFirst().grams());
  }

  @Test
  void aMistakenConsumptionCanBeUndone() {
    FoodItemId milk = stock(juan, "Milk", 892, 50, TODAY.plusDays(5), Visibility.SHARED);
    commands.execute(
        ana,
        new ConsumeFoodCommand(
            UUID.randomUUID(), household.id(), milk, Grams.of(800), MovementSource.MANUAL));

    new UndoLastChange(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            stores.catalog,
            snapshots,
            stores.audit,
            stores.unitOfWork,
            clock)
        .undo(ana, household.id());

    assertEquals(Grams.of(842), facade.snapshot(juan, household.id()).totalGrams());
  }

  @Test
  void tunaIsRejectedForAnaWhoIsAllergicToFish() {
    Recipe tunaSalad =
        new Recipe(
            RecipeId.newId(),
            "Tuna salad",
            2,
            10,
            RecipeSource.MANUAL,
            List.of(
                RecipeRequirement.of(
                    stores.catalog.findByName("Tuna").orElseThrow(), Grams.of(130))),
            List.of());
    DiningGroup table =
        DiningGroup.of(
            FoodProfile.omnivore(household.membershipOf(juan).orElseThrow().member()),
            FoodProfile.omnivore(household.membershipOf(ana).orElseThrow().member())
                .withAllergies(Set.of(Allergen.FISH)));

    assertFalse(table.canShare(tunaSalad));
    assertEquals(
        household.membershipOf(ana).orElseThrow().member(),
        table.conflictsWith(tunaSalad).getFirst().member());
  }
}
