package dev.haypacomer.application.analytics;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MovementHistory;
import dev.haypacomer.domain.analytics.WastePatterns;
import dev.haypacomer.domain.analytics.WasteTip;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class FindWastePatterns {

  public static final int MAX_DAYS = 90;

  private final GetHousehold households;
  private final MovementHistory history;
  private final Clock clock;

  public FindWastePatterns(HouseholdRepository households, MovementHistory history, Clock clock) {
    this.households = new GetHousehold(households);
    this.history = Objects.requireNonNull(history, "history");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<WasteTip> find(UserId actor, HouseholdId household, int days) {
    households.get(actor, household);
    if (days < 1 || days > MAX_DAYS) {
      throw new IllegalArgumentException("Look back 1 to " + MAX_DAYS + " days");
    }
    return WastePatterns.find(
        history.between(household, clock.instant().minus(Duration.ofDays(days)), clock.instant()));
  }
}
