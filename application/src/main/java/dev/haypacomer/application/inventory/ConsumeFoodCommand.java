package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ConsumeFoodCommand(
    UUID id, HouseholdId household, FoodItemId item, Grams grams, MovementSource source)
    implements InventoryCommand {

  public ConsumeFoodCommand {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(grams, "grams");
    Objects.requireNonNull(source, "source");
    if (grams.isZero()) {
      throw new IllegalArgumentException("Consumed grams must be positive");
    }
  }

  @Override
  public String action() {
    return "CONSUME_FOOD";
  }

  @Override
  public Map<String, String> detail() {
    return Map.of("grams", grams.value().toPlainString(), "source", source.name());
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
    Grams remaining = located.item().consume(grams);
    if (remaining.isZero()) {
      located.fridge().take(item);
    }
    workspace.inventory().save(household, located.fridge());
    workspace
        .inventory()
        .record(
            id,
            household,
            item,
            workspace.actor(),
            MovementType.CONSUME,
            grams.value().negate(),
            source,
            workspace.now());
    return new CommandOutcome(id, item, remaining, false);
  }
}
