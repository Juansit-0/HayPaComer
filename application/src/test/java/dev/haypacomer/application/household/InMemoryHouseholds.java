package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryHouseholds implements HouseholdRepository {

  final Map<HouseholdId, Household> byId = new LinkedHashMap<>();
  int saves;

  @Override
  public void save(Household household) {
    saves++;
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
