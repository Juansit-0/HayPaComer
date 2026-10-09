package dev.haypacomer.application.analytics;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.FoodNotInCatalogException;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class SetFoodPrice {

  public static final BigDecimal MAX_PRICE = new BigDecimal("100000000");

  private final GetHousehold households;
  private final FoodCatalogRepository catalog;
  private final FoodPriceRepository prices;

  public SetFoodPrice(
      HouseholdRepository households, FoodCatalogRepository catalog, FoodPriceRepository prices) {
    this.households = new GetHousehold(households);
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.prices = Objects.requireNonNull(prices, "prices");
  }

  public FoodMetadata set(
      UserId actor, HouseholdId household, String foodName, BigDecimal pricePerKg) {
    households.get(actor, household).requirePermission(actor, Permission.MANAGE_MARKET_LIST);
    if (pricePerKg.signum() <= 0 || pricePerKg.compareTo(MAX_PRICE) > 0) {
      throw new IllegalArgumentException("A price per kilogram must be positive and realistic");
    }
    FoodMetadata food =
        catalog.findByName(foodName).orElseThrow(() -> new FoodNotInCatalogException(foodName));
    prices.save(household, food.key(), pricePerKg.setScale(2, RoundingMode.HALF_UP));
    return food;
  }
}
