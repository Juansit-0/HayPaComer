package dev.haypacomer.application.port;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.inventory.InventoryMovement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryMovementLog {

  void record(InventoryMovement movement);

  List<InventoryMovement> history(FoodItemId item);

  Optional<InventoryMovement> findByCommand(UUID commandId);
}
