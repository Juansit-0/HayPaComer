package dev.haypacomer.application.inventory;

import dev.haypacomer.application.port.AuditLog;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.application.port.SnapshotStore;
import dev.haypacomer.application.port.UnitOfWork;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.UUID;

public final class TakeSnapshot {

  private final SnapshotServices services;

  public TakeSnapshot(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      SnapshotStore snapshots,
      AuditLog audit,
      UnitOfWork unitOfWork,
      Clock clock) {
    this.services =
        new SnapshotServices(
            households,
            fridges,
            ownerships,
            movements,
            catalog,
            snapshots,
            audit,
            unitOfWork,
            clock);
  }

  public InventorySnapshot take(UserId actor, HouseholdId household, String reason) {
    services.household(actor, household).requirePermission(actor, Permission.EDIT_INVENTORY);
    String label = reason == null || reason.isBlank() ? "Manual snapshot" : reason.strip();
    InventorySnapshot snapshot =
        new InventorySnapshot(
            UUID.randomUUID(),
            household,
            SnapshotKind.MANUAL,
            null,
            actor,
            label,
            services.caretaker.capture(household),
            services.clock.instant(),
            null);
    services.snapshots.save(snapshot);
    return snapshot;
  }
}
