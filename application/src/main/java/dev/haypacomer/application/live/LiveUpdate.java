package dev.haypacomer.application.live;

import dev.haypacomer.domain.household.HouseholdId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LiveUpdate(
    HouseholdId household, LiveUpdateKind kind, UUID fridge, String detail, Instant at) {

  public LiveUpdate {
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(detail, "detail");
    Objects.requireNonNull(at, "at");
  }

  public static LiveUpdate of(
      HouseholdId household, LiveUpdateKind kind, UUID fridge, String detail, Instant at) {
    return new LiveUpdate(household, kind, fridge, detail, at);
  }
}
