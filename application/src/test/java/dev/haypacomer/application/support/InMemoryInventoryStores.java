package dev.haypacomer.application.support;

import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.Ownership;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryInventoryStores {

  public final Map<FoodItemId, Ownership> ownershipById = new HashMap<>();
  public final List<InventoryMovement> movementLog = new ArrayList<>();
  public final Map<String, FoodMetadata> catalogByKey = new HashMap<>();

  public final FoodOwnershipRepository ownerships =
      new FoodOwnershipRepository() {
        @Override
        public void save(FoodItemId item, Ownership ownership) {
          ownershipById.put(item, ownership);
        }

        @Override
        public Optional<Ownership> find(FoodItemId item) {
          return Optional.ofNullable(ownershipById.get(item));
        }
      };

  public final InventoryMovementLog movements =
      new InventoryMovementLog() {
        @Override
        public void record(InventoryMovement movement) {
          movementLog.add(movement);
        }

        @Override
        public List<InventoryMovement> history(FoodItemId item) {
          return movementLog.stream().filter(movement -> movement.item().equals(item)).toList();
        }
      };

  public final FoodCatalogRepository catalog =
      new FoodCatalogRepository() {
        @Override
        public void save(FoodMetadata food) {
          catalogByKey.put(food.key(), food);
        }

        @Override
        public Optional<FoodMetadata> findByName(String name) {
          return Optional.ofNullable(catalogByKey.get(FoodMetadata.keyOf(name)));
        }

        @Override
        public List<FoodMetadata> search(String text, int limit) {
          return catalogByKey.values().stream()
              .filter(food -> food.key().startsWith(FoodMetadata.keyOf(text)))
              .limit(limit)
              .toList();
        }
      };
}
