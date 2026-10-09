package dev.haypacomer.persistence.resilience;

import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.domain.household.HouseholdId;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

public final class ResilientFoodPrices implements FoodPriceRepository {

  private final FoodPriceRepository delegate;
  private final ResilientReads reads;

  public ResilientFoodPrices(FoodPriceRepository delegate, ResilientReads reads) {
    this.delegate = delegate;
    this.reads = reads;
  }

  @Override
  public Map<String, BigDecimal> pricesFor(HouseholdId household, Currency currency) {
    return reads.read(
        household.value() + ":" + currency.getCurrencyCode(),
        () -> Map.copyOf(delegate.pricesFor(household, currency)));
  }

  @Override
  public void save(HouseholdId household, String foodKey, BigDecimal pricePerKg) {
    reads.write(
        () -> {
          delegate.save(household, foodKey, pricePerKg);
          return foodKey;
        });
  }
}
