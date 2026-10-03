package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record InventorySnapshot(
    UUID id,
    HouseholdId household,
    SnapshotKind kind,
    UUID commandId,
    UserId actor,
    String reason,
    InventoryMemento memento,
    Instant at,
    Instant usedAt) {

  public InventorySnapshot {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(actor, "actor");
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(memento, "memento");
    Objects.requireNonNull(at, "at");
  }

  public boolean isUsed() {
    return usedAt != null;
  }
}
