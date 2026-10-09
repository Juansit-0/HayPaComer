package dev.haypacomer.application.agent;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdMemory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ViewHouseholdMemory {

  private final GetHousehold households;
  private final HouseholdMemory memory;

  public ViewHouseholdMemory(HouseholdRepository households, HouseholdMemory memory) {
    this.households = new GetHousehold(households);
    this.memory = Objects.requireNonNull(memory, "memory");
  }

  public List<MemoryNote> view(UserId actor, HouseholdId household) {
    households.get(actor, household);
    return memory.read(household).entrySet().stream()
        .map(entry -> MemoryNote.fromEntry(entry.getKey(), entry.getValue()))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(MemoryNote::topic).thenComparing(MemoryNote::subject))
        .toList();
  }
}
