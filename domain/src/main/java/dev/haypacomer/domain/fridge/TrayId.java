package dev.haypacomer.domain.fridge;

import java.util.Objects;
import java.util.UUID;

public record TrayId(UUID value) {

  public TrayId {
    Objects.requireNonNull(value, "value");
  }

  public static TrayId newId() {
    return new TrayId(UUID.randomUUID());
  }
}
