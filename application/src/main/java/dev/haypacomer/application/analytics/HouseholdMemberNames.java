package dev.haypacomer.application.analytics;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Membership;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class HouseholdMemberNames {

  private final GetHousehold households;
  private final UserRepository users;

  public HouseholdMemberNames(HouseholdRepository households, UserRepository users) {
    this.households = new GetHousehold(households);
    this.users = Objects.requireNonNull(users, "users");
  }

  public Map<UserId, String> names(UserId actor, HouseholdId household) {
    Map<UserId, String> names = new HashMap<>();
    for (Membership membership : households.get(actor, household).memberships()) {
      users
          .findById(membership.user())
          .map(User::displayName)
          .ifPresent(name -> names.put(membership.user(), name));
    }
    return names;
  }
}
