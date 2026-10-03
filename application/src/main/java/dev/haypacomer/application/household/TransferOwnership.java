package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;

public final class TransferOwnership {

  private final MemberHouseholds households;

  public TransferOwnership(HouseholdRepository households) {
    this.households = new MemberHouseholds(households);
  }

  public Household transfer(UserId actor, HouseholdId id, UserId newOwner) {
    Household household = households.load(actor, id);
    household.transferOwnership(actor, newOwner);
    households.save(household);
    return household;
  }
}
