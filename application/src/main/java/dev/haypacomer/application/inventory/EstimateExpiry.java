package dev.haypacomer.application.inventory;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class EstimateExpiry {

  private final GetHousehold households;
  private final FoodCatalogRepository catalog;
  private final ExpiryDesk expiry;
  private final Clock clock;

  public EstimateExpiry(
      HouseholdRepository households,
      FoodCatalogRepository catalog,
      ExpiryDesk expiry,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.expiry = Objects.requireNonNull(expiry, "expiry");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public ExpiryEstimate estimate(
      UserId actor, HouseholdId householdId, String foodName, ZoneKind zone, boolean opened) {
    Household household = households.get(actor, householdId);
    FoodMetadata food =
        catalog.findByName(foodName).orElseThrow(() -> new FoodNotInCatalogException(foodName));
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    return expiry.estimate(householdId, food, zone, opened, today);
  }
}
