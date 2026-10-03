package dev.haypacomer.application.port;

import dev.haypacomer.application.inventory.InventorySnapshot;
import dev.haypacomer.application.inventory.SnapshotKind;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SnapshotStore {

  void save(InventorySnapshot snapshot);

  Optional<InventorySnapshot> find(HouseholdId household, UUID id);

  Optional<InventorySnapshot> latestUnusedUndo(HouseholdId household);

  List<InventorySnapshot> list(HouseholdId household, SnapshotKind kind, int limit);

  void markUsed(UUID id, Instant at);

  void closeUndoHistory(HouseholdId household, Instant at);
}
