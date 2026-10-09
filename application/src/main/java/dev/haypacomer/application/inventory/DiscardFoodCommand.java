package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record DiscardFoodCommand(UUID id, HouseholdId household, FoodItemId item)
    implements InventoryCommand {

  public DiscardFoodCommand {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(item, "item");
  }

  @Override
  public String action() {
    return "DISCARD_FOOD";
  }

  @Override
  public Map<String, String> detail() {
    return Map.of();
  }

  @Override
  public CommandOutcome execute(InventoryWorkspace workspace) {
    HouseholdInventory.Located located = workspace.inventory().locate(household, item);
    workspace
        .guard()
        .requireUsable(
            workspace.household(),
            workspace.actor(),
            workspace.inventory().ownerships().find(item));
    FoodItem removed = located.fridge().take(item);
    workspace.inventory().save(household, located.fridge());
    workspace
        .inventory()
        .record(
            id,
            household,
            removed,
            workspace.actor(),
            MovementType.DISCARD,
            removed.quantity().value().negate(),
            MovementSource.MANUAL,
            workspace.now());
    return new CommandOutcome(id, item, Grams.ZERO, false);
  }
}
