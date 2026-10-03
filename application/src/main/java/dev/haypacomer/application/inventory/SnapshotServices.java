package dev.haypacomer.application.inventory;

import dev.haypacomer.application.audit.AuditEntry;
import dev.haypacomer.application.port.AuditLog;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.application.port.SnapshotStore;
import dev.haypacomer.application.port.UnitOfWork;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

final class SnapshotServices {

  final HouseholdInventory inventory;
  final InventoryCaretaker caretaker;
  final SnapshotStore snapshots;
  final AuditLog audit;
  final UnitOfWork unitOfWork;
  final Clock clock;

  SnapshotServices(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      SnapshotStore snapshots,
      AuditLog audit,
      UnitOfWork unitOfWork,
      Clock clock) {
    this.inventory = new HouseholdInventory(households, fridges, ownerships, movements);
    this.caretaker = new InventoryCaretaker(inventory, catalog);
    this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
    this.audit = Objects.requireNonNull(audit, "audit");
    this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  Household household(UserId actor, HouseholdId id) {
    return inventory.household(actor, id);
  }

  void restore(UserId actor, InventorySnapshot snapshot, String action) {
    Instant now = clock.instant();
    caretaker.restore(snapshot.household(), snapshot.memento());
    snapshots.markUsed(snapshot.id(), now);
    audit.record(
        new AuditEntry(
            actor,
            snapshot.household(),
            action,
            "SNAPSHOT",
            snapshot.id(),
            Map.of("reason", snapshot.reason()),
            now));
  }
}
