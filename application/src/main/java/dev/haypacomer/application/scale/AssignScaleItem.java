package dev.haypacomer.application.scale;

import dev.haypacomer.application.inventory.FoodAccessGuard;
import dev.haypacomer.application.inventory.FoodItemNotFoundException;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.ScaleAssignmentRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;

public final class AssignScaleItem {

  private final ScaleAccess access;
  private final HouseholdRepository households;
  private final FridgeRepository fridges;
  private final FoodOwnershipRepository ownerships;
  private final FoodAccessGuard guard;
  private final ScaleAssignmentRepository assignments;
  private final Clock clock;

  public AssignScaleItem(
      HouseholdRepository households,
      DeviceRepository devices,
      ScaleSampleStore samples,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      FoodAccessGuard guard,
      ScaleAssignmentRepository assignments,
      Clock clock) {
    this.access = new ScaleAccess(households, devices, samples, clock);
    this.households = Objects.requireNonNull(households, "households");
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.ownerships = Objects.requireNonNull(ownerships, "ownerships");
    this.guard = Objects.requireNonNull(guard, "guard");
    this.assignments = Objects.requireNonNull(assignments, "assignments");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public ScaleAssignment assign(
      UserId actor, HouseholdId householdId, DeviceId deviceId, FoodItemId item) {
    Device scale = access.scale(actor, householdId, deviceId, Permission.MANAGE_OWN_ITEMS);
    boolean present =
        fridges.findByHousehold(householdId).stream()
            .anyMatch(fridge -> fridge.findItem(item).isPresent());
    if (!present) {
      throw new FoodItemNotFoundException();
    }
    Household household = households.findById(householdId).orElseThrow();
    guard.requireUsable(household, actor, ownerships.find(item));
    ScaleAssignment assignment =
        new ScaleAssignment(scale.id(), householdId, item, actor, clock.instant());
    assignments.save(assignment);
    return assignment;
  }
}
