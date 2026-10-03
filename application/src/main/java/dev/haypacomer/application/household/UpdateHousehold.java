package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;

public final class UpdateHousehold {

  private final MemberHouseholds households;

  public UpdateHousehold(HouseholdRepository households) {
    this.households = new MemberHouseholds(households);
  }

  public Household update(UserId actor, HouseholdId id, UpdateHouseholdCommand command) {
    Household household = households.load(actor, id);
    if (command.name() != null) {
      household.rename(actor, command.name());
    }
    if (command.currency() != null || command.timezone() != null) {
      household.configure(
          actor,
          command.currency() == null ? household.currency() : Settings.currency(command.currency()),
          command.timezone() == null
              ? household.timezone()
              : Settings.timezone(command.timezone()));
    }
    households.save(household);
    return household;
  }
}
