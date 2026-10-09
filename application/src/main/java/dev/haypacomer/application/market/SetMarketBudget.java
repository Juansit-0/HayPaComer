package dev.haypacomer.application.market;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketBudgetRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class SetMarketBudget {

  public static final BigDecimal MAX = new BigDecimal("10000000000");

  private final GetHousehold households;
  private final MarketBudgetRepository budgets;

  public SetMarketBudget(HouseholdRepository households, MarketBudgetRepository budgets) {
    this.households = new GetHousehold(households);
    this.budgets = Objects.requireNonNull(budgets, "budgets");
  }

  public BigDecimal set(UserId actor, HouseholdId household, BigDecimal monthly) {
    households.get(actor, household).requirePermission(actor, Permission.MANAGE_MARKET_LIST);
    if (monthly.signum() <= 0 || monthly.compareTo(MAX) > 0) {
      throw new IllegalArgumentException("A monthly budget must be positive and realistic");
    }
    BigDecimal amount = monthly.setScale(2, RoundingMode.HALF_UP);
    budgets.save(household, amount);
    return amount;
  }
}
