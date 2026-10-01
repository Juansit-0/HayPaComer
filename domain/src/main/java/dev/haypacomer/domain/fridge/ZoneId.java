package dev.haypacomer.domain.fridge;

import java.util.Objects;
import java.util.UUID;

public record ZoneId(UUID value) {

  public ZoneId {
    Objects.requireNonNull(value, "value");
  }

  public static ZoneId newId() {
    return new ZoneId(UUID.randomUUID());
  }
}
