package dev.haypacomer.application.port;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.inventory.InventoryMovement;
import java.util.List;

public interface InventoryMovementLog {

  void record(InventoryMovement movement);

  List<InventoryMovement> history(FoodItemId item);
}
