package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.FridgeMemento;
import dev.haypacomer.domain.inventory.Ownership;
import java.util.List;
import java.util.Map;

public record InventoryMemento(List<FridgeMemento> fridges, Map<FoodItemId, Ownership> ownerships) {

  public InventoryMemento {
    fridges = List.copyOf(fridges);
    ownerships = Map.copyOf(ownerships);
  }
}
