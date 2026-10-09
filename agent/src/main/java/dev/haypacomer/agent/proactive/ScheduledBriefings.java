package dev.haypacomer.agent.proactive;

import dev.haypacomer.application.analytics.BuildWeeklyDigest;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.BriefingLog;
import dev.haypacomer.application.port.HouseholdDirectory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.settings.BriefingSchedule;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.inventory.FoodStatus;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public final class ScheduledBriefings {

  private final HouseholdDirectory directory;
  private final HouseholdRepository households;
  private final ViewInventory inventory;
  private final BriefingLog log;
  private final Briefer briefer;
  private final BuildWeeklyDigest digest;
  private final PolicySource policies;
  private final Clock clock;
  private int failures;

  public ScheduledBriefings(
      HouseholdDirectory directory,
      HouseholdRepository households,
      ViewInventory inventory,
      BriefingLog log,
      Briefer briefer,
      BuildWeeklyDigest digest,
      Clock clock) {
    this(directory, households, inventory, log, briefer, digest, FixedPolicies.DEFAULT, clock);
  }

  public ScheduledBriefings(
      HouseholdDirectory directory,
      HouseholdRepository households,
      ViewInventory inventory,
      BriefingLog log,
      Briefer briefer,
      BuildWeeklyDigest digest,
      PolicySource policies,
      Clock clock) {
    this.directory = directory;
    this.households = households;
    this.inventory = inventory;
    this.log = log;
    this.briefer = briefer;
    this.digest = digest;
    this.policies = policies;
    this.clock = clock;
  }

  public int run() {
    int sent = 0;
    for (HouseholdId id : directory.all()) {
      try {
        sent += briefHousehold(id);
      } catch (RuntimeException failure) {
        failures++;
      }
    }
    return sent;
  }

  public int failures() {
    return failures;
  }

  private int briefHousehold(HouseholdId id) {
    Household household = households.findById(id).orElse(null);
    if (household == null) {
      return 0;
    }
    int sent = 0;
    ZonedDateTime local = clock.instant().atZone(household.timezone());
    LocalDate today = local.toLocalDate();
    BriefingSchedule schedule = policies.briefings(id);
    if (local.getHour() == schedule.morningHour() && log.claim(id, "daily", today)) {
      sent +=
          briefer
                  .brief(
                      id,
                      "chef",
                      "Morning briefing: what should we cook and use first today?",
                      "Today in your kitchen")
                  .isPresent()
              ? 1
              : 0;
    }
    if (local.getDayOfWeek() == DayOfWeek.MONDAY
        && local.getHour() == schedule.digestHour()
        && log.claim(id, "weekly-digest", today)) {
      briefer.publish(id, "Your week in the kitchen", digest.build(household.owner(), id).text());
      sent++;
    }
    long expiring =
        inventory.view(household.owner(), id, today).stream()
            .filter(InventoryEntry::usable)
            .filter(entry -> entry.food().has(FoodStatus.AT_RISK))
            .count();
    if (expiring >= schedule.expiryClusterSize() && log.claim(id, "expiry-cluster", today)) {
      sent +=
          briefer
                  .brief(
                      id,
                      "coach",
                      expiring + " foods expire soon: how do we avoid wasting them?",
                      expiring + " foods expire soon")
                  .isPresent()
              ? 1
              : 0;
    }
    return sent;
  }
}
