package dev.haypacomer.application.support;

import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryFridgeRepository implements FridgeRepository {

  private final Map<FridgeId, Fridge> fridges = new LinkedHashMap<>();
  private final Map<FridgeId, HouseholdId> owners = new LinkedHashMap<>();

  @Override
  public void save(HouseholdId household, Fridge fridge) {
    fridges.put(fridge.id(), fridge);
    owners.put(fridge.id(), household);
  }

  @Override
  public Optional<Fridge> findById(FridgeId id) {
    return Optional.ofNullable(fridges.get(id));
  }

  @Override
  public List<Fridge> findByHousehold(HouseholdId household) {
    return fridges.values().stream()
        .filter(fridge -> owners.get(fridge.id()).equals(household))
        .toList();
  }
}
