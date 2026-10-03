package dev.haypacomer.domain.inventory;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InventoryMovement(
    UUID commandId,
    HouseholdId household,
    FoodItemId item,
    UserId actor,
    MovementType type,
    BigDecimal deltaGrams,
    MovementSource source,
    Instant at) {

  public InventoryMovement {
    Objects.requireNonNull(commandId, "commandId");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(actor, "actor");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(deltaGrams, "deltaGrams");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(at, "at");
  }
}
