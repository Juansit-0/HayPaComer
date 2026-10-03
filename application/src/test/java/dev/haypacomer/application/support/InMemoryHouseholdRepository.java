package dev.haypacomer.application.support;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryHouseholdRepository implements HouseholdRepository {

  private final Map<HouseholdId, Household> byId = new LinkedHashMap<>();

  @Override
  public void save(Household household) {
    byId.put(household.id(), household);
  }

  @Override
  public Optional<Household> findById(HouseholdId id) {
    return Optional.ofNullable(byId.get(id));
  }

  @Override
  public List<Household> findByUser(UserId user) {
    return byId.values().stream()
        .filter(household -> household.membershipOf(user).isPresent())
        .toList();
  }
}
