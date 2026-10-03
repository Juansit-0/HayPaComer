package dev.haypacomer.application.inventory;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

final class HouseholdInventory {

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final FoodOwnershipRepository ownerships;
  private final InventoryMovementLog movements;

  HouseholdInventory(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.ownerships = Objects.requireNonNull(ownerships, "ownerships");
    this.movements = Objects.requireNonNull(movements, "movements");
  }

  Household household(UserId actor, HouseholdId id) {
    return households.get(actor, id);
  }

  Located locate(HouseholdId household, FoodItemId item) {
    for (Fridge fridge : fridges.findByHousehold(household)) {
      var found = fridge.findItem(item);
      if (found.isPresent()) {
        return new Located(fridge, found.get());
      }
    }
    throw new FoodItemNotFoundException();
  }

  FoodOwnershipRepository ownerships() {
    return ownerships;
  }

  void save(HouseholdId household, Fridge fridge) {
    fridges.save(household, fridge);
  }

  void record(
      HouseholdId household,
      FoodItemId item,
      UserId actor,
      MovementType type,
      BigDecimal delta,
      MovementSource source,
      Instant at) {
    movements.record(
        new InventoryMovement(UUID.randomUUID(), household, item, actor, type, delta, source, at));
  }

  record Located(Fridge fridge, FoodItem item) {}
}
