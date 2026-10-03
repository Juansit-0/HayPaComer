package dev.haypacomer.domain.identity;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }

  public static UserId newId() {
    return new UserId(UUID.randomUUID());
  }
}
