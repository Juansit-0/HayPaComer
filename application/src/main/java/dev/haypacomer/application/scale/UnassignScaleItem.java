package dev.haypacomer.application.scale;

import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.ScaleAssignmentRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;

public final class UnassignScaleItem {

  private final ScaleAccess access;
  private final ScaleAssignmentRepository assignments;

  public UnassignScaleItem(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      ScaleAssignmentRepository assignments,
      Clock clock) {
    this.access = new ScaleAccess(households, devices, samples, clock);
    this.assignments = Objects.requireNonNull(assignments, "assignments");
  }

  public void unassign(UserId actor, HouseholdId household, DeviceId device) {
    assignments.remove(access.scale(actor, household, device, Permission.MANAGE_OWN_ITEMS).id());
  }
}
