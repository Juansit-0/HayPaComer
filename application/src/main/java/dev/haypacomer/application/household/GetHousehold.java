package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;

public final class GetHousehold {

  private final MemberHouseholds households;

  public GetHousehold(HouseholdRepository households) {
    this.households = new MemberHouseholds(households);
  }

  public Household get(UserId actor, HouseholdId id) {
    return households.load(actor, id);
  }
}
