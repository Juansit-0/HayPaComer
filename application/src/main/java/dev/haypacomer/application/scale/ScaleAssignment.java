package dev.haypacomer.application.scale;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.Objects;

public record ScaleAssignment(
    DeviceId scale, HouseholdId household, FoodItemId item, UserId assignedBy, Instant assignedAt) {

  public ScaleAssignment {
    Objects.requireNonNull(scale, "scale");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(assignedBy, "assignedBy");
    Objects.requireNonNull(assignedAt, "assignedAt");
  }
}
