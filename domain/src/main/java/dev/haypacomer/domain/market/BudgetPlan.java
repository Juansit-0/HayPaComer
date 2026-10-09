package dev.haypacomer.domain.market;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record BudgetPlan(
    BigDecimal monthly,
    BigDecimal spent,
    BigDecimal remaining,
    BigDecimal plannedCost,
    List<BudgetLine> lines) {

  public BudgetPlan {
    Objects.requireNonNull(monthly, "monthly");
    Objects.requireNonNull(spent, "spent");
    Objects.requireNonNull(remaining, "remaining");
    Objects.requireNonNull(plannedCost, "plannedCost");
    lines = List.copyOf(lines);
  }

  public List<BudgetLine> overBudget() {
    return lines.stream().filter(line -> line.cost().isPresent() && !line.withinBudget()).toList();
  }

  public List<BudgetLine> unpriced() {
    return lines.stream().filter(line -> line.cost().isEmpty()).toList();
  }
}
