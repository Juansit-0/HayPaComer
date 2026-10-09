package dev.haypacomer.application.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.audit.ListActivity;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Grant;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.Revoke;
import dev.haypacomer.application.inventory.ChangeFoodOwnership.SetVisibility;
import dev.haypacomer.application.live.BroadcastLiveUpdate;
import dev.haypacomer.application.live.LiveUpdate;
import dev.haypacomer.application.live.LiveUpdateKind;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.application.support.InMemorySnapshotStore;
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
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

  private ExecuteInventoryCommand commands;
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
    commands =
        new ExecuteInventoryCommand(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            stores.catalog,
            guard,
            stores.audit,
            stores.unitOfWork,
            new InMemorySnapshotStore(),
            new BroadcastLiveUpdate(List.of(live::add)),
            clock);
    changeOwnership =
        new ChangeFoodOwnership(households, fridges, stores.ownerships, stores.movements);
    viewInventory =
        new ViewInventory(
            households,
            fridges,
            stores.ownerships,
            new InMemoryColdChainRepository(),
            FreshnessPolicy.DEFAULT);
  }

  private final List<LiveUpdate> live = new ArrayList<>();

  private FoodItem stock(UserId actor, String food, long gross, long tare, Visibility visibility) {
    CommandOutcome outcome =
        commands.execute(
            actor,
            new StockFoodCommand(
                UUID.randomUUID(),
                household.id(),
                fridge.id(),
                rack.id(),
                food,
                Grams.of(gross),
                Grams.of(tare),
                TODAY.plusDays(5),
                visibility));
    return fridge.findItem(outcome.item()).orElseThrow();
  }

  private Grams consume(UserId actor, FoodItemId item, long grams, MovementSource source) {
    return commands
        .execute(
            actor,
            new ConsumeFoodCommand(
                UUID.randomUUID(), household.id(), item, Grams.of(grams), source))
        .remaining();
  }

  private void discard(UserId actor, FoodItemId item) {
    commands.execute(actor, new DiscardFoodCommand(UUID.randomUUID(), household.id(), item));
  }

  @Test
  void stocksWeighedFoodAndLogsTheAddition() {
    FoodItem milk = stock(juan, "milk", 892, 50, null);

    assertEquals(Grams.of(842), milk.quantity());
    assertEquals(Visibility.SHARED, stores.ownershipById.get(milk.id()).visibility());
    InventoryMovement added = stores.movementLog.getFirst();
    assertEquals(MovementType.ADD, added.type());
    assertEquals(0, new BigDecimal("842").compareTo(added.deltaGrams()));
    assertEquals("milk", added.food().orElseThrow());
    consume(juan, milk.id(), 842, MovementSource.SCALE);
    assertEquals("milk", stores.movementLog.getLast().food().orElseThrow());
  }

  @Test
  void rejectsUnknownFoodsFridgesAndGuestsSharingFood() {
    assertThrows(FoodNotInCatalogException.class, () -> stock(juan, "Dragon fruit", 100, 0, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            commands.execute(
                juan,
                new StockFoodCommand(
                    UUID.randomUUID(),
                    household.id(),
                    FridgeId.newId(),
                    rack.id(),
                    "Milk",
                    Grams.of(1),
                    null,
                    null,
                    null)));
    assertThrows(
        AccessDeniedException.class, () -> stock(guest, "Yogurt", 125, 0, Visibility.SHARED));
    assertEquals(Grams.of(125), stock(guest, "Yogurt", 125, 0, Visibility.PRIVATE).quantity());
  }

  @Test
  void consumesMeasuredGramsAndRemovesEmptyItems() {
    FoodItem milk = stock(juan, "Milk", 892, 50, null);

    assertEquals(Grams.of(650), consume(ana, milk.id(), 192, MovementSource.SCALE));
    assertEquals(Grams.ZERO, consume(ana, milk.id(), 650, MovementSource.MANUAL));
    assertTrue(fridge.findItem(milk.id()).isEmpty());
    assertEquals(
        List.of(MovementType.ADD, MovementType.CONSUME, MovementType.CONSUME),
        stores.movements.history(milk.id()).stream().map(InventoryMovement::type).toList());
    assertThrows(
        FoodItemNotFoundException.class, () -> consume(ana, milk.id(), 1, MovementSource.MANUAL));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ConsumeFoodCommand(
                UUID.randomUUID(),
                household.id(),
                FoodItemId.newId(),
                Grams.ZERO,
                MovementSource.MANUAL));
  }

  @Test
  void privateFoodIsProtectedUntilTheOwnerGrantsAccess() {
    FoodItem yogurt = stock(juan, "Yogurt", 125, 0, Visibility.PRIVATE);

    assertThrows(
        AccessDeniedException.class, () -> consume(ana, yogurt.id(), 50, MovementSource.MANUAL));
    assertThrows(
        AccessDeniedException.class,
        () -> changeOwnership.change(ana, household.id(), yogurt.id(), new Grant(ana)));

    changeOwnership.change(juan, household.id(), yogurt.id(), new Grant(ana));
    assertEquals(Grams.of(75), consume(ana, yogurt.id(), 50, MovementSource.MANUAL));

    changeOwnership.change(juan, household.id(), yogurt.id(), new Revoke(ana));
    assertThrows(AccessDeniedException.class, () -> discard(ana, yogurt.id()));
    assertThrows(
        IllegalArgumentException.class,
        () -> changeOwnership.change(juan, household.id(), yogurt.id(), new Grant(UserId.newId())));
  }

  @Test
  void askFirstAsksAndSharingOpensTheFood() {
    FoodItem yogurt = stock(juan, "Yogurt", 125, 0, Visibility.ASK_FIRST);

    assertThrows(
        PermissionRequiredException.class,
        () -> consume(ana, yogurt.id(), 10, MovementSource.MANUAL));

    changeOwnership.change(juan, household.id(), yogurt.id(), new SetVisibility(Visibility.SHARED));
    discard(ana, yogurt.id());

    assertTrue(fridge.findItem(yogurt.id()).isEmpty());
    assertEquals(0, new BigDecimal("-125").compareTo(stores.movementLog.getLast().deltaGrams()));
    assertEquals(MovementType.DISCARD, stores.movementLog.getLast().type());
    assertEquals("yogurt", stores.movementLog.getLast().food().orElseThrow());
  }

  @Test
  void replayedCommandsDoNotApplyTwice() {
    FoodItem milk = stock(juan, "Milk", 892, 50, null);
    ConsumeFoodCommand consume =
        new ConsumeFoodCommand(
            UUID.randomUUID(), household.id(), milk.id(), Grams.of(192), MovementSource.SCALE);

    CommandOutcome first = commands.execute(ana, consume);
    CommandOutcome retry = commands.execute(ana, consume);

    assertFalse(first.replayed());
    assertTrue(retry.replayed());
    assertEquals(Grams.of(650), retry.remaining());
    assertEquals(Grams.of(650), milk.quantity());
    assertEquals(2, stores.movements.history(milk.id()).size());
  }

  @Test
  void everyCommandIsAuditedInsideAUnitOfWork() {
    FoodItem milk = stock(juan, "Milk", 892, 50, null);
    consume(ana, milk.id(), 192, MovementSource.SCALE);
    discard(ana, milk.id());

    assertEquals(3, stores.unitsOfWork);
    assertEquals(
        List.of("STOCK_FOOD", "CONSUME_FOOD", "DISCARD_FOOD"),
        stores.auditEntries.stream().map(entry -> entry.action()).toList());
    assertEquals("192.00", stores.auditEntries.get(1).detail().get("grams"));
    assertEquals("650.00", stores.auditEntries.get(1).detail().get("remainingGrams"));
    assertEquals(milk.id().value(), stores.auditEntries.get(2).entityId());
    assertEquals(
        List.of("DISCARD_FOOD", "CONSUME_FOOD"),
        new ListActivity(households, stores.audit)
            .list(juan, household.id(), 2).stream().map(entry -> entry.action()).toList());
  }

  @Test
  void failedCommandsAreNotAudited() {
    FoodItem yogurt = stock(juan, "Yogurt", 125, 0, Visibility.PRIVATE);

    assertThrows(
        AccessDeniedException.class, () -> consume(ana, yogurt.id(), 10, MovementSource.MANUAL));

    assertEquals(1, stores.auditEntries.size());
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
    assertEquals("Private food", forAna.getLast().food().item().name());
    assertEquals(Grams.ZERO, forAna.getLast().food().item().quantity());
    assertTrue(forJuan.stream().allMatch(InventoryEntry::usable));
    assertTrue(forJuan.stream().anyMatch(entry -> entry.food().item().name().equals("Yogurt")));
  }

  @Test
  void everyRealChangeIsBroadcastLiveButReplaysAreNot() {
    FoodItem milk = stock(juan, "Milk", 892, 50, null);
    UUID command = UUID.randomUUID();
    commands.execute(
        juan,
        new ConsumeFoodCommand(
            command, household.id(), milk.id(), Grams.of(200), MovementSource.MANUAL));
    commands.execute(
        juan,
        new ConsumeFoodCommand(
            command, household.id(), milk.id(), Grams.of(200), MovementSource.MANUAL));

    assertEquals(2, live.size());
    assertEquals(LiveUpdateKind.INVENTORY, live.getLast().kind());
    assertEquals(household.id(), live.getLast().household());
    assertTrue(live.getLast().detail().endsWith("642.00 g left"));
  }
}
