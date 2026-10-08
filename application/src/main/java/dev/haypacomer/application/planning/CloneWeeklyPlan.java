package dev.haypacomer.application.planning;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.WeeklyPlanRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.planning.WeeklyPlanId;
import java.time.LocalDate;
import java.util.Objects;

public final class CloneWeeklyPlan {

  private final GetHousehold households;
  private final WeeklyPlanRepository plans;

  public CloneWeeklyPlan(HouseholdRepository households, WeeklyPlanRepository plans) {
    this.households = new GetHousehold(households);
    this.plans = Objects.requireNonNull(plans, "plans");
  }

  public WeeklyPlan clone(
      UserId actor, HouseholdId householdId, WeeklyPlanId planId, LocalDate weekStart) {
    households.get(actor, householdId).requirePermission(actor, Permission.COOK);
    WeeklyPlan copy =
        plans
            .find(householdId, planId)
            .orElseThrow(WeeklyPlanNotFoundException::new)
            .cloneFor(weekStart);
    plans.save(copy);
    return copy;
  }
}
