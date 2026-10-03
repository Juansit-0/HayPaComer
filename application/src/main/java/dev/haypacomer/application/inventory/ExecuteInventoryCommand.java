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
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ExecuteInventoryCommand {

  private final HouseholdInventory inventory;
  private final FoodAccessGuard guard;
  private final FoodCatalogRepository catalog;
  private final AuditLog audit;
  private final UnitOfWork unitOfWork;
  private final InventoryCaretaker caretaker;
  private final SnapshotStore snapshots;
  private final Clock clock;

  public ExecuteInventoryCommand(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodCatalogRepository catalog,
      FoodAccessGuard guard,
      AuditLog audit,
      UnitOfWork unitOfWork,
      SnapshotStore snapshots,
      Clock clock) {
    this.inventory = new HouseholdInventory(households, fridges, ownerships, movements);
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.guard = Objects.requireNonNull(guard, "guard");
    this.audit = Objects.requireNonNull(audit, "audit");
    this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
    this.caretaker = new InventoryCaretaker(inventory, catalog);
    this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public CommandOutcome execute(UserId actor, InventoryCommand command) {
    Household household = inventory.household(actor, command.household());
    return unitOfWork.run(
        () -> {
          Optional<InventoryMovement> previous = inventory.movement(command.id());
          if (previous.isPresent()) {
            return replay(previous.get());
          }
          Instant now = clock.instant();
          InventoryMemento before = caretaker.capture(command.household());
          CommandOutcome outcome =
              command.execute(
                  new InventoryWorkspace(actor, household, inventory, guard, catalog, now));
          Map<String, String> detail = new HashMap<>(command.detail());
          detail.put("commandId", command.id().toString());
          detail.put("remainingGrams", outcome.remaining().value().toPlainString());
          audit.record(
              new AuditEntry(
                  actor,
                  command.household(),
                  command.action(),
                  "FOOD_ITEM",
                  outcome.item().value(),
                  detail,
                  now));
          snapshots.save(
              new InventorySnapshot(
                  UUID.randomUUID(),
                  command.household(),
                  SnapshotKind.UNDO,
                  command.id(),
                  actor,
                  command.action(),
                  before,
                  now,
                  null));
          return outcome;
        });
  }

  private CommandOutcome replay(InventoryMovement movement) {
    Grams remaining =
        inventory
            .find(movement.household(), movement.item())
            .map(FoodItem::quantity)
            .orElse(Grams.ZERO);
    return new CommandOutcome(movement.commandId(), movement.item(), remaining, true);
  }
}
