package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.household.HouseholdId;
import java.util.Map;
import java.util.UUID;

public sealed interface InventoryCommand
    permits StockFoodCommand, ConsumeFoodCommand, DiscardFoodCommand {

  UUID id();

  HouseholdId household();

  String action();

  Map<String, String> detail();

  CommandOutcome execute(InventoryWorkspace workspace);
}
