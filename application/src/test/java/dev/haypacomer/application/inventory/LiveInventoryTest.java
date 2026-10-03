package dev.haypacomer.application.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Grant;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Revoke;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.SetVisibility;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FoodStatus;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.math.BigDecimal;
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

class LiveInventoryTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final FoodAccessGuard guard = new FoodAccessGuard();
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final UserId guest = UserId.newId();
  private Household household;
  private Fridge fridge;
  private Tray rack;

  private StockFood stockFood;
  private ConsumeFood consumeFood;
  private DiscardFood discardFood;
  private ChangeFoodOwnership changeOwnership;
  private ViewInventory viewInventory;

  @BeforeEach
  void createKitchen() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    rack = fridge.trays().filter(tray -> tray.name().equals("Rack")).findFirst().orElseThrow();
    stores.catalog.save(
        new FoodMetadata(
            "Milk",
            FoodCategory.DAIRY,
            Unit.MILLILITER,
            ConversionFactors.withDensity("1.03"),
            true,
            7,
            Set.of(Allergen.MILK)));
    stores.catalog.save(
        new FoodMetadata(
            "Yogurt",
            FoodCategory.DAIRY,
            Unit.GRAM,
            ConversionFactors.MASS_ONLY,
            true,
            10,
            Set.of()));
    stockFood =
        new StockFood(
            households, fridges, stores.ownerships, stores.movements, stores.catalog, clock);
    consumeFood =
        new ConsumeFood(households, fridges, stores.ownerships, stores.movements, guard, clock);
    discardFood =
        new DiscardFood(households, fridges, stores.ownerships, stores.movements, guard, clock);
    changeOwnership =
        new ChangeFoodOwnership(households, fridges, stores.ownerships, stores.movements);
    viewInventory =
        new ViewInventory(households, fridges, stores.ownerships, FreshnessPolicy.DEFAULT);
  }

  private FoodItem stock(UserId actor, String food, long gross, long tare, Visibility visibility) {
    return stockFood.stock(
        actor,
        household.id(),
        new StockCommand(
            fridge.id(),
            rack.id(),
            food,
            Grams.of(gross),
            Grams.of(tare),
            TODAY.plusDays(5),
            visibility));
  }

  @Test
  void stocksWeighedFoodAndLogsTheAddition() {
    FoodItem milk = stock(juan, "milk", 892, 50, null);

    assertEquals(Grams.of(842), milk.quantity());
    assertEquals(Visibility.SHARED, stores.ownershipById.get(milk.id()).visibility());
    InventoryMovement added = stores.movementLog.getFirst();
    assertEquals(MovementType.ADD, added.type());
    assertEquals(0, new BigDecimal("842").compareTo(added.deltaGrams()));
  }

  @Test
  void rejectsUnknownFoodsFridgesAndGuestsSharingFood() {
    assertThrows(FoodNotInCatalogException.class, () -> stock(juan, "Dragon fruit", 100, 0, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            stockFood.stock(
                juan,
                household.id(),
                new StockCommand(
                    FridgeId.newId(), rack.id(), "Milk", Grams.of(1), null, null, null)));
    assertThrows(
        AccessDeniedException.class, () -> stock(guest, "Yogurt", 125, 0, Visibility.SHARED));
    assertEquals(Grams.of(125), stock(guest, "Yogurt", 125, 0, Visibility.PRIVATE).quantity());
  }

  @Test
  void consumesMeasuredGramsAndRemovesEmptyItems() {
    FoodItem milk = stock(juan, "Milk", 892, 50, null);

    assertEquals(
        Grams.of(650),
        consumeFood.consume(ana, household.id(), milk.id(), Grams.of(192), MovementSource.SCALE));
    assertEquals(
        Grams.ZERO,
        consumeFood.consume(ana, household.id(), milk.id(), Grams.of(650), MovementSource.MANUAL));
    assertTrue(fridge.findItem(milk.id()).isEmpty());
    assertEquals(
        List.of(MovementType.ADD, MovementType.CONSUME, MovementType.CONSUME),
        stores.movements.history(milk.id()).stream().map(InventoryMovement::type).toList());
    assertThrows(
        FoodItemNotFoundException.class,
        () ->
            consumeFood.consume(
                ana, household.id(), milk.id(), Grams.of(1), MovementSource.MANUAL));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            consumeFood.consume(
                ana, household.id(), FoodItemId.newId(), Grams.ZERO, MovementSource.MANUAL));
  }

  @Test
  void privateFoodIsProtectedUntilTheOwnerGrantsAccess() {
    FoodItem yogurt = stock(juan, "Yogurt", 125, 0, Visibility.PRIVATE);

    assertThrows(
        AccessDeniedException.class,
        () ->
            consumeFood.consume(
                ana, household.id(), yogurt.id(), Grams.of(50), MovementSource.MANUAL));
    assertThrows(
        AccessDeniedException.class,
        () -> changeOwnership.change(ana, household.id(), yogurt.id(), new Grant(ana)));

    changeOwnership.change(juan, household.id(), yogurt.id(), new Grant(ana));
    assertEquals(
        Grams.of(75),
        consumeFood.consume(ana, household.id(), yogurt.id(), Grams.of(50), MovementSource.MANUAL));

    changeOwnership.change(juan, household.id(), yogurt.id(), new Revoke(ana));
    assertThrows(
        AccessDeniedException.class, () -> discardFood.discard(ana, household.id(), yogurt.id()));
    assertThrows(
        IllegalArgumentException.class,
        () -> changeOwnership.change(juan, household.id(), yogurt.id(), new Grant(UserId.newId())));
  }

  @Test
  void askFirstAsksAndSharingOpensTheFood() {
    FoodItem yogurt = stock(juan, "Yogurt", 125, 0, Visibility.ASK_FIRST);

    assertThrows(
        PermissionRequiredException.class,
        () ->
            consumeFood.consume(
                ana, household.id(), yogurt.id(), Grams.of(10), MovementSource.MANUAL));

    changeOwnership.change(juan, household.id(), yogurt.id(), new SetVisibility(Visibility.SHARED));
    FoodItem discarded = discardFood.discard(ana, household.id(), yogurt.id());

    assertEquals(Grams.of(125), discarded.quantity());
    assertEquals(MovementType.DISCARD, stores.movementLog.getLast().type());
  }

  @Test
  void inventoryShowsOwnershipAndUsabilityPerViewer() {
    stock(juan, "Yogurt", 125, 0, Visibility.PRIVATE);
    stock(juan, "Milk", 892, 50, null);

    List<InventoryEntry> forAna = viewInventory.view(ana, household.id(), TODAY);
    List<InventoryEntry> forJuan = viewInventory.view(juan, household.id(), TODAY);

    assertEquals("Milk", forAna.getFirst().food().item().name());
    assertFalse(forAna.getLast().usable());
    assertTrue(forAna.getLast().food().has(FoodStatus.PRIVATE));
    assertTrue(forJuan.stream().allMatch(InventoryEntry::usable));
  }
}
