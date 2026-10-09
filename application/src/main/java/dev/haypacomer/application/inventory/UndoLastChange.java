package dev.haypacomer.application.inventory;

import dev.haypacomer.application.port.AuditLog;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.application.port.SnapshotStore;
import dev.haypacomer.application.port.UnitOfWork;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;

public final class UndoLastChange {

  private final SnapshotServices services;

  public UndoLastChange(
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

  public InventorySnapshot undo(UserId actor, HouseholdId householdId) {
    Household household = services.household(actor, householdId);
    return services.unitOfWork.runFor(
        householdId,
        () -> {
          InventorySnapshot latest =
              services
                  .snapshots
                  .latestUnusedUndo(householdId)
                  .orElseThrow(NothingToUndoException::new);
          if (!latest.actor().equals(actor) && !household.owner().equals(actor)) {
            throw new AccessDeniedException("Only its author or the owner can undo this change");
          }
          services.restore(actor, latest, "UNDO_" + latest.reason());
          return latest;
        });
  }
}
