package dev.haypacomer.application.port;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.inventory.Ownership;
import java.util.Optional;

public interface FoodOwnershipRepository {

  void save(FoodItemId item, Ownership ownership);

  Optional<Ownership> find(FoodItemId item);
}
