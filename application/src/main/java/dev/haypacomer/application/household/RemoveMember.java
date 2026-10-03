package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;

public final class RemoveMember {

  private final MemberHouseholds households;

  public RemoveMember(HouseholdRepository households) {
    this.households = new MemberHouseholds(households);
  }

  public void remove(UserId actor, HouseholdId id, UserId target) {
    Household household = households.load(actor, id);
    household.remove(actor, target);
    households.save(household);
  }
}
