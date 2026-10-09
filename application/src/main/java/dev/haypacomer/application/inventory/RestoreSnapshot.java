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

public final class RestoreSnapshot {

  private final SnapshotServices services;

  public RestoreSnapshot(
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

  public InventorySnapshot restore(UserId actor, HouseholdId household, UUID snapshotId) {
    services.household(actor, household).requirePermission(actor, Permission.MANAGE_HOUSEHOLD);
    return services.unitOfWork.runFor(
        household,
        () -> {
          InventorySnapshot snapshot =
              services
                  .snapshots
                  .find(household, snapshotId)
                  .filter(found -> found.kind() == SnapshotKind.MANUAL)
                  .orElseThrow(SnapshotNotFoundException::new);
          services.restore(actor, snapshot, "RESTORE_SNAPSHOT");
          services.snapshots.closeUndoHistory(household, services.clock.instant());
          return snapshot;
        });
  }
}
