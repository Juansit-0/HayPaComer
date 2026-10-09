package dev.haypacomer.application.market;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketBudgetRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.BudgetPlanner;
import dev.haypacomer.domain.market.MarketList;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class ViewMarketBudget {

  private final GetHousehold households;
  private final MarketBudgetRepository budgets;
  private final MarketListRepository lists;
  private final FoodPriceRepository prices;
  private final SubstitutionRuleRepository rules;
  private final Clock clock;

  public ViewMarketBudget(
      HouseholdRepository households,
      MarketBudgetRepository budgets,
      MarketListRepository lists,
      FoodPriceRepository prices,
      SubstitutionRuleRepository rules,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.budgets = Objects.requireNonNull(budgets, "budgets");
    this.lists = Objects.requireNonNull(lists, "lists");
    this.prices = Objects.requireNonNull(prices, "prices");
    this.rules = Objects.requireNonNull(rules, "rules");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public BudgetReport view(UserId actor, HouseholdId householdId) {
    Household household = households.get(actor, householdId);
    var monthly = budgets.monthly(householdId).orElseThrow(MarketBudgetNotSetException::new);
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    return new BudgetReport(
        household.currency(),
        new BudgetPlanner()
            .plan(
                lists.findByHousehold(householdId).orElseGet(() -> MarketList.empty(householdId)),
                monthly,
                today.withDayOfMonth(1).atStartOfDay(household.timezone()).toInstant(),
                prices.pricesFor(householdId, household.currency()),
                rules.all()));
  }
}
