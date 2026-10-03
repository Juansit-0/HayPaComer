package dev.haypacomer.application.inventory;

import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Clock;
import java.util.Objects;

public final class ConsumeFood {

  private final HouseholdInventory inventory;
  private final FoodAccessGuard guard;
  private final Clock clock;

  public ConsumeFood(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements,
      FoodAccessGuard guard,
      Clock clock) {
    this.inventory = new HouseholdInventory(households, fridges, ownerships, movements);
    this.guard = Objects.requireNonNull(guard, "guard");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Grams consume(
      UserId actor,
      HouseholdId householdId,
      FoodItemId itemId,
      Grams grams,
      MovementSource source) {
    if (grams.isZero()) {
      throw new IllegalArgumentException("Consumed grams must be positive");
    }
    Household household = inventory.household(actor, householdId);
    HouseholdInventory.Located located = inventory.locate(householdId, itemId);
    guard.requireUsable(household, actor, inventory.ownerships().find(itemId));
    Grams remaining = located.item().consume(grams);
    if (remaining.isZero()) {
      located.fridge().take(itemId);
    }
    inventory.save(householdId, located.fridge());
    inventory.record(
        householdId,
        itemId,
        actor,
        MovementType.CONSUME,
        grams.value().negate(),
        source,
        clock.instant());
    return remaining;
  }
}
