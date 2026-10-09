package dev.haypacomer.application.coldchain;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.SensorHistory;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.domain.coldchain.investigation.ColdInvestigation;
import dev.haypacomer.domain.coldchain.investigation.ColdInvestigator;
import dev.haypacomer.domain.coldchain.investigation.FoodAssessment;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class InvestigateColdIncidents {

  public static final Duration MAX_LOOKBACK = Duration.ofDays(7);

  private final GetHousehold households;
  private final FridgeRepository fridges;
  private final SensorHistory history;
  private final ViewInventory inventory;
  private final PolicySource policies;
  private final Clock clock;

  public InvestigateColdIncidents(
      HouseholdRepository households,
      FridgeRepository fridges,
      SensorHistory history,
      ViewInventory inventory,
      Clock clock) {
    this(households, fridges, history, inventory, FixedPolicies.DEFAULT, clock);
  }

  public InvestigateColdIncidents(
      HouseholdRepository households,
      FridgeRepository fridges,
      SensorHistory history,
      ViewInventory inventory,
      PolicySource policies,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.fridges = Objects.requireNonNull(fridges, "fridges");
    this.history = Objects.requireNonNull(history, "history");
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.policies = Objects.requireNonNull(policies, "policies");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<ColdInvestigation> investigate(
      UserId actor, HouseholdId householdId, Optional<FridgeId> only, Duration lookback) {
    Household household = households.get(actor, householdId);
    if (lookback.isNegative() || lookback.isZero() || lookback.compareTo(MAX_LOOKBACK) > 0) {
      throw new IllegalArgumentException("Look back between 1 minute and 7 days");
    }
    List<Fridge> chosen =
        fridges.findByHousehold(householdId).stream()
            .filter(fridge -> only.map(fridge.id()::equals).orElse(true))
            .toList();
    if (only.isPresent() && chosen.isEmpty()) {
      throw new FridgeNotFoundException();
    }
    Instant to = clock.instant();
    Instant from = to.minus(lookback);
    Set<FoodItemId> usable =
        inventory.view(actor, householdId, LocalDate.ofInstant(to, household.timezone())).stream()
            .filter(InventoryEntry::usable)
            .map(entry -> entry.food().item().id())
            .collect(Collectors.toSet());
    return chosen.stream()
        .map(
            fridge -> {
              ColdInvestigation found =
                  new ColdInvestigator(policies.thresholds(householdId), policies.coldRule())
                      .investigate(
                          fridge.id(),
                          from,
                          to,
                          history.doorAndTemperature(fridge.id(), from, to),
                          items(fridge));
              return new ColdInvestigation(
                  found.fridge(),
                  found.from(),
                  found.to(),
                  found.readings(),
                  found.episodes(),
                  found.totalAboveLimit(),
                  found.foods().stream().map(assessment -> privacy(assessment, usable)).toList());
            })
        .toList();
  }

  private static List<FoodItem> items(Fridge fridge) {
    return StreamSupport.stream(fridge.spliterator(), false)
        .filter(FoodItem.class::isInstance)
        .map(FoodItem.class::cast)
        .toList();
  }

  private static FoodAssessment privacy(FoodAssessment assessment, Set<FoodItemId> usable) {
    if (usable.contains(assessment.item())) {
      return assessment;
    }
    return new FoodAssessment(
        assessment.item(),
        "Private food",
        Grams.ZERO,
        assessment.verdict(),
        assessment.reason() + "; tell its owner");
  }
}
