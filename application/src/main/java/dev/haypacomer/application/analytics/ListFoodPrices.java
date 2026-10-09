package dev.haypacomer.application.analytics;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class ListFoodPrices {

  private final GetHousehold households;
  private final FoodPriceRepository prices;

  public ListFoodPrices(HouseholdRepository households, FoodPriceRepository prices) {
    this.households = new GetHousehold(households);
    this.prices = Objects.requireNonNull(prices, "prices");
  }

  public Map<String, BigDecimal> list(UserId actor, HouseholdId householdId) {
    Household household = households.get(actor, householdId);
    return new TreeMap<>(prices.pricesFor(householdId, household.currency()));
  }
}
