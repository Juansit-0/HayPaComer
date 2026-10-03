package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

final class MemberHouseholds {

  private final HouseholdRepository households;

  MemberHouseholds(HouseholdRepository households) {
    this.households = Objects.requireNonNull(households, "households");
  }

  Household load(UserId actor, HouseholdId id) {
    return households
        .findById(id)
        .filter(household -> household.membershipOf(actor).isPresent())
        .orElseThrow(HouseholdNotFoundException::new);
  }

  void save(Household household) {
    households.save(household);
  }
}
