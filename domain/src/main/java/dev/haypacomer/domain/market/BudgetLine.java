package dev.haypacomer.domain.market;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record BudgetLine(
    MarketItem item, BigDecimal estimatedCost, boolean withinBudget, CheaperOption cheaper) {

  public BudgetLine {
    Objects.requireNonNull(item, "item");
  }

  public Optional<BigDecimal> cost() {
    return Optional.ofNullable(estimatedCost);
  }

  public Optional<CheaperOption> cheaperOption() {
    return Optional.ofNullable(cheaper);
  }
}
