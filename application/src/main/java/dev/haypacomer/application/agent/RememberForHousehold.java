package dev.haypacomer.application.agent;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdMemory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public final class RememberForHousehold {

  public static final int MAX_NOTES = 200;

  private final GetHousehold households;
  private final HouseholdMemory memory;

  public RememberForHousehold(HouseholdRepository households, HouseholdMemory memory) {
    this.households = new GetHousehold(households);
    this.memory = Objects.requireNonNull(memory, "memory");
  }

  public MemoryNote remember(UserId actor, HouseholdId household, MemoryNote note) {
    households.get(actor, household).requirePermission(actor, Permission.COOK);
    var current = memory.read(household);
    if (!current.containsKey(note.key()) && current.size() >= MAX_NOTES) {
      throw new IllegalStateException("Household memory is full; forget something first");
    }
    memory.remember(household, note.key(), note.value());
    return note;
  }
}
