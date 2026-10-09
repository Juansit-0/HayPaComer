package dev.haypacomer.application.agent;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdMemory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public final class ClearHouseholdMemory {

  private final GetHousehold households;
  private final HouseholdMemory memory;

  public ClearHouseholdMemory(HouseholdRepository households, HouseholdMemory memory) {
    this.households = new GetHousehold(households);
    this.memory = Objects.requireNonNull(memory, "memory");
  }

  public int clear(UserId actor, HouseholdId household) {
    households.get(actor, household).requirePermission(actor, Permission.MANAGE_HOUSEHOLD);
    var keys = memory.read(household).keySet();
    keys.forEach(key -> memory.forget(household, key));
    return keys.size();
  }
}
