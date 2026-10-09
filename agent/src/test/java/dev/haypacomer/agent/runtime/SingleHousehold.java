package dev.haypacomer.agent.runtime;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Optional;

record SingleHousehold(Household household) implements HouseholdRepository {

  @Override
  public void save(Household updated) {}

  @Override
  public Optional<Household> findById(HouseholdId id) {
    return Optional.of(household).filter(found -> found.id().equals(id));
  }

  @Override
  public List<Household> findByUser(UserId user) {
    return household.membershipOf(user).isPresent() ? List.of(household) : List.of();
  }
}
