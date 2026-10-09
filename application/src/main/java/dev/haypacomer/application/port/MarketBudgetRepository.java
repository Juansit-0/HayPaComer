package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import java.math.BigDecimal;
import java.util.Optional;

public interface MarketBudgetRepository {

  Optional<BigDecimal> monthly(HouseholdId household);

  void save(HouseholdId household, BigDecimal monthly);
}
