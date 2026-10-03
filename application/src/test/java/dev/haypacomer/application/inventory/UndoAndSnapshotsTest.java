package dev.haypacomer.application.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.application.support.InMemorySnapshotStore;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UndoAndSnapshotsTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final InMemorySnapshotStore snapshots = new InMemorySnapshotStore();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final UserId leo = UserId.newId();
  private Household household;
  private Fridge fridge;
  private Tray rack;
  private ExecuteInventoryCommand commands;
  private UndoLastChange undo;
  private TakeSnapshot takeSnapshot;
  private RestoreSnapshot restoreSnapshot;

  @BeforeEach
  void createKitchen() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    household.join(leo, Role.MEMBER, NOW);
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
            Set.of()));
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
    undo =
        new UndoLastChange(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            stores.catalog,
            snapshots,
            stores.audit,
            stores.unitOfWork,
            clock);
    takeSnapshot =
        new TakeSnapshot(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            stores.catalog,
            snapshots,
            stores.audit,
            stores.unitOfWork,
            clock);
    restoreSnapshot =
        new RestoreSnapshot(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            stores.catalog,
            snapshots,
            stores.audit,
            stores.unitOfWork,
            clock);
  }

  private FoodItemId stock(UserId actor, long grams, Visibility visibility) {
    return commands
        .execute(
            actor,
            new StockFoodCommand(
                UUID.randomUUID(),
                household.id(),
                fridge.id(),
                rack.id(),
                "Milk",
                Grams.of(grams),
                Grams.ZERO,
                null,
                visibility))
        .item();
  }

  private void consume(UserId actor, FoodItemId item, long grams) {
    commands.execute(
        actor,
        new ConsumeFoodCommand(
            UUID.randomUUID(), household.id(), item, Grams.of(grams), MovementSource.MANUAL));
  }

  private Grams quantity(FoodItemId item) {
    return fridges
        .findById(fridge.id())
        .orElseThrow()
        .findItem(item)
        .map(i -> i.quantity())
        .orElse(Grams.ZERO);
  }

  private boolean present(FoodItemId item) {
    return fridges.findById(fridge.id()).orElseThrow().findItem(item).isPresent();
  }

  @Test
  void undoesTheLastChangesStepByStep() {
    FoodItemId milk = stock(juan, 842, Visibility.PRIVATE);
    consume(juan, milk, 192);
    consume(juan, milk, 650);
    assertTrue(!present(milk));

    assertEquals("CONSUME_FOOD", undo.undo(juan, household.id()).reason());
    assertEquals(Grams.of(650), quantity(milk));
    undo.undo(juan, household.id());
    assertEquals(Grams.of(842), quantity(milk));
    assertEquals(Visibility.PRIVATE, stores.ownershipById.get(milk).visibility());
    undo.undo(juan, household.id());
    assertTrue(!present(milk));
    assertThrows(NothingToUndoException.class, () -> undo.undo(juan, household.id()));
    assertEquals("UNDO_STOCK_FOOD", stores.auditEntries.getLast().action());
  }

  @Test
  void onlyTheAuthorOrTheOwnerCanUndo() {
    FoodItemId milk = stock(ana, 842, Visibility.SHARED);

    assertThrows(AccessDeniedException.class, () -> undo.undo(leo, household.id()));
    undo.undo(juan, household.id());
    assertTrue(!present(milk));

    stock(ana, 500, Visibility.SHARED);
    undo.undo(ana, household.id());
  }

  @Test
  void ownerRestoresAManualSnapshotAndUndoHistoryCloses() {
    FoodItemId milk = stock(juan, 842, Visibility.SHARED);
    InventorySnapshot snapshot = takeSnapshot.take(ana, household.id(), "  before party  ");
    consume(ana, milk, 800);
    FoodItemId extra = stock(ana, 300, Visibility.SHARED);

    assertThrows(
        AccessDeniedException.class,
        () -> restoreSnapshot.restore(ana, household.id(), snapshot.id()));
    restoreSnapshot.restore(juan, household.id(), snapshot.id());

    assertEquals(Grams.of(842), quantity(milk));
    assertTrue(!present(extra));
    assertThrows(NothingToUndoException.class, () -> undo.undo(juan, household.id()));
    assertEquals(
        List.of("before party"),
        new ListSnapshots(households, snapshots)
            .list(ana, household.id()).stream().map(InventorySnapshot::reason).toList());
    assertEquals("Manual snapshot", takeSnapshot.take(juan, household.id(), " ").reason());
    assertThrows(
        SnapshotNotFoundException.class,
        () -> restoreSnapshot.restore(juan, household.id(), UUID.randomUUID()));
  }
}
