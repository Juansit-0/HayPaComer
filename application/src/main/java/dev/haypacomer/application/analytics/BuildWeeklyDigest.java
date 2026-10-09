package dev.haypacomer.application.analytics;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MovementHistory;
import dev.haypacomer.domain.analytics.WastePatterns;
import dev.haypacomer.domain.analytics.WeeklyDigest;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class BuildWeeklyDigest {

  private final GetHousehold households;
  private final ViewHouseholdMetrics metrics;
  private final MovementHistory history;
  private final HouseholdMemberNames names;
  private final Clock clock;

  public BuildWeeklyDigest(
      HouseholdRepository households,
      ViewHouseholdMetrics metrics,
      MovementHistory history,
      HouseholdMemberNames names,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.metrics = Objects.requireNonNull(metrics, "metrics");
    this.history = Objects.requireNonNull(history, "history");
    this.names = Objects.requireNonNull(names, "names");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public WeeklyDigest build(UserId actor, HouseholdId householdId) {
    Household household = households.get(actor, householdId);
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    LocalDate weekStart = today.minusDays(7);
    MetricsReport thisWeek = metrics.view(actor, householdId, weekStart, today.minusDays(1));
    MetricsReport lastWeek =
        metrics.view(actor, householdId, today.minusDays(14), today.minusDays(8));
    return new WeeklyDigest(
        thisWeek.metrics(),
        lastWeek.metrics(),
        WastePatterns.find(
            history.between(
                householdId,
                weekStart.atStartOfDay(household.timezone()).toInstant(),
                today.atStartOfDay(household.timezone()).toInstant())),
        thisWeek.currency(),
        names.names(actor, householdId));
  }
}
