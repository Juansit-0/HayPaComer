package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.TrayId;
import dev.haypacomer.domain.inventory.StockedFood;
import java.util.Objects;

public record InventoryEntry(FridgeId fridge, TrayId tray, StockedFood food, boolean usable) {

  public InventoryEntry {
    Objects.requireNonNull(fridge, "fridge");
    Objects.requireNonNull(tray, "tray");
    Objects.requireNonNull(food, "food");
  }
}
