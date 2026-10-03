package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;

public final class CreateHousehold {

  private final HouseholdRepository households;
  private final Clock clock;

  public CreateHousehold(HouseholdRepository households, Clock clock) {
    this.households = Objects.requireNonNull(households, "households");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public Household create(UserId actor, CreateHouseholdCommand command) {
    Household household =
        Household.create(
            command.name(),
            Settings.currency(command.currency()),
            Settings.timezone(command.timezone()),
            actor,
            clock.instant());
    households.save(household);
    return household;
  }
}
