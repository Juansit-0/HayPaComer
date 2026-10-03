package dev.haypacomer.application.support;

import dev.haypacomer.application.inventory.InventorySnapshot;
import dev.haypacomer.application.inventory.SnapshotKind;
import dev.haypacomer.application.port.SnapshotStore;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class InMemorySnapshotStore implements SnapshotStore {

  public final List<InventorySnapshot> saved = new ArrayList<>();

  @Override
  public void save(InventorySnapshot snapshot) {
    saved.removeIf(existing -> existing.id().equals(snapshot.id()));
    saved.add(snapshot);
  }

  @Override
  public Optional<InventorySnapshot> find(HouseholdId household, UUID id) {
    return saved.stream()
        .filter(snapshot -> snapshot.household().equals(household) && snapshot.id().equals(id))
        .findFirst();
  }

  @Override
  public Optional<InventorySnapshot> latestUnusedUndo(HouseholdId household) {
    return saved.reversed().stream()
        .filter(snapshot -> snapshot.household().equals(household))
        .filter(snapshot -> snapshot.kind() == SnapshotKind.UNDO && !snapshot.isUsed())
        .findFirst();
  }

  @Override
  public List<InventorySnapshot> list(HouseholdId household, SnapshotKind kind, int limit) {
    return saved.reversed().stream()
        .filter(snapshot -> snapshot.household().equals(household) && snapshot.kind() == kind)
        .limit(limit)
        .toList();
  }

  @Override
  public void markUsed(UUID id, Instant at) {
    saved.replaceAll(snapshot -> snapshot.id().equals(id) ? used(snapshot, at) : snapshot);
  }

  @Override
  public void closeUndoHistory(HouseholdId household, Instant at) {
    saved.replaceAll(
        snapshot ->
            snapshot.household().equals(household)
                    && snapshot.kind() == SnapshotKind.UNDO
                    && !snapshot.isUsed()
                ? used(snapshot, at)
                : snapshot);
  }

  private static InventorySnapshot used(InventorySnapshot snapshot, Instant at) {
    return new InventorySnapshot(
        snapshot.id(),
        snapshot.household(),
        snapshot.kind(),
        snapshot.commandId(),
        snapshot.actor(),
        snapshot.reason(),
        snapshot.memento(),
        snapshot.at(),
        at);
  }
}
