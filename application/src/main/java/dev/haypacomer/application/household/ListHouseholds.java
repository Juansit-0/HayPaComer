package dev.haypacomer.application.household;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Objects;

public final class ListHouseholds {

  private final HouseholdRepository households;

  public ListHouseholds(HouseholdRepository households) {
    this.households = Objects.requireNonNull(households, "households");
  }

  public List<Household> list(UserId actor) {
    return households.findByUser(actor);
  }
}
