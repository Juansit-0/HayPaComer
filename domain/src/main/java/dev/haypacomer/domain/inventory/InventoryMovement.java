package dev.haypacomer.domain.inventory;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record InventoryMovement(
    UUID commandId,
    HouseholdId household,
    FoodItemId item,
    UserId actor,
    MovementType type,
    BigDecimal deltaGrams,
    MovementSource source,
    Instant at,
    String foodKey,
    LocalDate expiresOn) {

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

  public InventoryMovement(
      UUID commandId,
      HouseholdId household,
      FoodItemId item,
      UserId actor,
      MovementType type,
      BigDecimal deltaGrams,
      MovementSource source,
      Instant at) {
    this(commandId, household, item, actor, type, deltaGrams, source, at, null, null);
  }

  public Optional<String> food() {
    return Optional.ofNullable(foodKey);
  }

  public Optional<LocalDate> expiry() {
    return Optional.ofNullable(expiresOn);
  }
}
