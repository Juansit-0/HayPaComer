package dev.haypacomer.domain.household;

import java.util.Objects;
import java.util.UUID;

public record HouseholdId(UUID value) {

  public HouseholdId {
    Objects.requireNonNull(value, "value");
  }

  public static HouseholdId newId() {
    return new HouseholdId(UUID.randomUUID());
  }
}
