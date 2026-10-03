package dev.haypacomer.application.inventory;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.SnapshotStore;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Objects;

public final class ListSnapshots {

  private static final int LIMIT = 50;

  private final GetHousehold households;
  private final SnapshotStore snapshots;

  public ListSnapshots(HouseholdRepository households, SnapshotStore snapshots) {
    this.households = new GetHousehold(households);
    this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
  }

  public List<InventorySnapshot> list(UserId actor, HouseholdId household) {
    households.get(actor, household);
    return snapshots.list(household, SnapshotKind.MANUAL, LIMIT);
  }
}
