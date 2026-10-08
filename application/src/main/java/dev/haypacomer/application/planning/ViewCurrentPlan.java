package dev.haypacomer.application.planning;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.WeeklyPlanRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.planning.WeeklyPlan;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class ViewCurrentPlan {

  private final GetHousehold households;
  private final WeeklyPlanRepository plans;
  private final Clock clock;

  public ViewCurrentPlan(HouseholdRepository households, WeeklyPlanRepository plans, Clock clock) {
    this.households = new GetHousehold(households);
    this.plans = Objects.requireNonNull(plans, "plans");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public WeeklyPlan view(UserId actor, HouseholdId householdId) {
    Household household = households.get(actor, householdId);
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    return plans.current(householdId, today).orElseThrow(WeeklyPlanNotFoundException::new);
  }
}
