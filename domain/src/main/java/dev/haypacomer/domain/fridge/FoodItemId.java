package dev.haypacomer.domain.fridge;

import java.util.Objects;
import java.util.UUID;

public record FoodItemId(UUID value) {

  public FoodItemId {
    Objects.requireNonNull(value, "value");
  }

  public static FoodItemId newId() {
    return new FoodItemId(UUID.randomUUID());
  }
}
