package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

public interface FoodPriceRepository {

  Map<String, BigDecimal> pricesFor(HouseholdId household, Currency currency);

  void save(HouseholdId household, String foodKey, BigDecimal pricePerKg);
}
