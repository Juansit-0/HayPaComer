package dev.haypacomer.domain.fridge;

import java.util.Objects;
import java.util.UUID;

public record FridgeId(UUID value) {

  public FridgeId {
    Objects.requireNonNull(value, "value");
  }

  public static FridgeId newId() {
    return new FridgeId(UUID.randomUUID());
  }
}
