package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.inventory.InventoryMemento;
import dev.haypacomer.application.inventory.InventorySnapshot;
import dev.haypacomer.application.inventory.SnapshotKind;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.Grams;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresSnapshotStoreTest extends PostgresTestSupport {

  private User juan;
  private Household household;
  private InventoryMemento memento;

  @BeforeEach
  void createData() {
    juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    Fridge fridge = Fridge.named("Kitchen");
    Zone door = Zone.named("Door", ZoneKind.DOOR);
    Tray rack = Tray.named("Rack", 0);
    door.add(rack);
    fridge.add(door);
    FoodItem milk =
        new FoodItem(
            FoodItemId.newId(), MILK, Grams.of("842.50"), Grams.of(50), LocalDate.of(2026, 10, 9));
    fridge.place(milk, rack.id());
    MemberId owner = household.membershipOf(juan.id()).orElseThrow().member();
    memento =
        new InventoryMemento(
            List.of(fridge.snapshot()),
            Map.of(milk.id(), Ownership.of(owner, Visibility.PRIVATE).grant(MemberId.newId())));
  }

  private InventorySnapshot snapshot(SnapshotKind kind, String reason) {
    return new InventorySnapshot(
        UUID.randomUUID(),
        household.id(),
        kind,
        UUID.randomUUID(),
        juan.id(),
        reason,
        memento,
        NOW,
        null);
  }

  @Test
  void roundTripsTheMementoAsJson() {
    PostgresSnapshotStore store = new PostgresSnapshotStore(dataSource);
    InventorySnapshot manual = snapshot(SnapshotKind.MANUAL, "before party");

    store.save(manual);

    assertEquals(manual, store.find(household.id(), manual.id()).orElseThrow());
    assertEquals(List.of(manual), store.list(household.id(), SnapshotKind.MANUAL, 10));
  }

  @Test
  void returnsTheNewestUnusedUndoAndClosesHistory() {
    PostgresSnapshotStore store = new PostgresSnapshotStore(dataSource);
    InventorySnapshot first = snapshot(SnapshotKind.UNDO, "STOCK_FOOD");
    InventorySnapshot second = snapshot(SnapshotKind.UNDO, "CONSUME_FOOD");
    store.save(first);
    store.save(second);

    assertEquals(second.id(), store.latestUnusedUndo(household.id()).orElseThrow().id());
    store.markUsed(second.id(), NOW.plusSeconds(5));
    assertEquals(first.id(), store.latestUnusedUndo(household.id()).orElseThrow().id());
    store.closeUndoHistory(household.id(), NOW.plusSeconds(10));
    assertTrue(store.latestUnusedUndo(household.id()).isEmpty());
    assertTrue(store.find(household.id(), first.id()).orElseThrow().isUsed());
  }
}
