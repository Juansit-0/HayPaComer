package dev.haypacomer.application.market;

import dev.haypacomer.domain.market.BudgetPlan;
import java.util.Currency;
import java.util.Objects;

public record BudgetReport(Currency currency, BudgetPlan plan) {

  public BudgetReport {
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(plan, "plan");
  }
}
