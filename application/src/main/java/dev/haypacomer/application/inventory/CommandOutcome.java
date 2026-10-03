package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Objects;
import java.util.UUID;

public record CommandOutcome(UUID commandId, FoodItemId item, Grams remaining, boolean replayed) {

  public CommandOutcome {
    Objects.requireNonNull(commandId, "commandId");
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(remaining, "remaining");
  }
}
