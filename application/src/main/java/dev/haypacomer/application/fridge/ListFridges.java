package dev.haypacomer.application.fridge;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Objects;

public final class ListFridges {

  private final GetHousehold households;
  private final FridgeRepository fridges;

  public ListFridges(HouseholdRepository households, FridgeRepository fridges) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
  }

  public List<Fridge> list(UserId actor, HouseholdId householdId) {
    households.get(actor, householdId);
    return fridges.findByHousehold(householdId);
  }
}
