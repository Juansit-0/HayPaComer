package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Membership;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;

public final class ChangeMemberRole {

  private final MemberHouseholds households;

  public ChangeMemberRole(HouseholdRepository households) {
    this.households = new MemberHouseholds(households);
  }

  public Membership change(UserId actor, HouseholdId id, UserId target, Role role) {
    Household household = households.load(actor, id);
    Membership membership = household.changeRole(actor, target, role);
    households.save(household);
    return membership;
  }
}
