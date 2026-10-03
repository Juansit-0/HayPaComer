package dev.haypacomer.application.fridge;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public final class SetUpFridge {

  private final GetHousehold households;
  private final FridgeRepository fridges;

  public SetUpFridge(HouseholdRepository households, FridgeRepository fridges) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
  }

  public Fridge setUp(UserId actor, HouseholdId householdId, String name, FridgeLayout layout) {
    households.get(actor, householdId).requirePermission(actor, Permission.MANAGE_FRIDGES);
    Fridge fridge = Objects.requireNonNull(layout, "layout").build(name);
    fridges.save(householdId, fridge);
    return fridge;
  }
}
