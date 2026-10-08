package dev.haypacomer.domain.planning;

import java.util.Objects;
import java.util.UUID;

public record WeeklyPlanId(UUID value) {

  public WeeklyPlanId {
    Objects.requireNonNull(value, "value");
  }

  public static WeeklyPlanId newId() {
    return new WeeklyPlanId(UUID.randomUUID());
  }
}
