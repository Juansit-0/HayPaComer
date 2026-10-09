package dev.haypacomer.application.analytics;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MovementHistory;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.domain.analytics.MetricsCalculator;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class ViewHouseholdMetrics {

  public static final int MAX_DAYS = 366;

  private final GetHousehold households;
  private final MovementHistory history;
  private final FoodPriceRepository prices;
  private final PolicySource policies;

  public ViewHouseholdMetrics(
      HouseholdRepository households,
      MovementHistory history,
      FoodPriceRepository prices,
      FreshnessPolicy freshness) {
    this(households, history, prices, FixedPolicies.DEFAULT.withFreshness(freshness));
  }

  public ViewHouseholdMetrics(
      HouseholdRepository households,
      MovementHistory history,
      FoodPriceRepository prices,
      PolicySource policies) {
    this.households = new GetHousehold(households);
    this.history = Objects.requireNonNull(history, "history");
    this.prices = Objects.requireNonNull(prices, "prices");
    this.policies = Objects.requireNonNull(policies, "policies");
  }

  public MetricsReport view(UserId actor, HouseholdId householdId, LocalDate from, LocalDate to) {
    Household household = households.get(actor, householdId);
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
      throw new IllegalArgumentException("Choose a period of 1 to " + MAX_DAYS + " days");
    }
    return new MetricsReport(
        new MetricsCalculator(policies.freshness(householdId))
            .calculate(
                history.between(
                    householdId,
                    from.atStartOfDay(household.timezone()).toInstant(),
                    to.plusDays(1).atStartOfDay(household.timezone()).toInstant()),
                prices.pricesFor(householdId, household.currency()),
                household.timezone(),
                from,
                to),
        household.currency());
  }
}
