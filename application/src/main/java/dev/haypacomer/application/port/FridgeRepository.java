package dev.haypacomer.application.port;

import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.util.List;
import java.util.Optional;

public interface FridgeRepository {

  void save(HouseholdId household, Fridge fridge);

  Optional<Fridge> findById(FridgeId id);

  List<Fridge> findByHousehold(HouseholdId household);
}
