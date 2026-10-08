package dev.haypacomer.domain.planning;

import java.util.Objects;
import java.util.UUID;

public record PlanEntryId(UUID value) {

  public PlanEntryId {
    Objects.requireNonNull(value, "value");
  }

  public static PlanEntryId newId() {
    return new PlanEntryId(UUID.randomUUID());
  }
}
