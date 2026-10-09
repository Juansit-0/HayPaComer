package dev.haypacomer.domain.market;

import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BudgetPlanner {

  private static final BigDecimal KILO = BigDecimal.valueOf(1000);

  public BudgetPlan plan(
      MarketList list,
      BigDecimal monthly,
      Instant monthStart,
      Map<String, BigDecimal> pricePerKg,
      List<SubstitutionRule> rules) {
    if (monthly.signum() < 0) {
      throw new IllegalArgumentException("A budget cannot be negative");
    }
    BigDecimal spent =
        list.items().stream()
            .filter(item -> item.checkedAt() != null && !item.checkedAt().isBefore(monthStart))
            .map(item -> cost(item.food().key(), item.grams(), pricePerKg))
            .flatMap(Optional::stream)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal remaining = monthly.subtract(spent).max(BigDecimal.ZERO);
    BigDecimal left = remaining;
    BigDecimal planned = BigDecimal.ZERO;
    List<BudgetLine> lines = new ArrayList<>();
    for (MarketItem item : prioritized(list.pending())) {
      Optional<BigDecimal> cost = cost(item.food().key(), item.grams(), pricePerKg);
      if (cost.isEmpty()) {
        lines.add(new BudgetLine(item, null, false, null));
        continue;
      }
      boolean fits = cost.get().compareTo(left) <= 0;
      CheaperOption cheaper = cheaper(item, cost.get(), pricePerKg, rules).orElse(null);
      if (fits) {
        left = left.subtract(cost.get());
        planned = planned.add(cost.get());
      }
      lines.add(new BudgetLine(item, cost.get(), fits, cheaper));
    }
    return new BudgetPlan(
        scaled(monthly), scaled(spent), scaled(remaining), scaled(planned), lines);
  }

  private static List<MarketItem> prioritized(List<MarketItem> pending) {
    return pending.stream()
        .sorted(
            Comparator.comparingInt((MarketItem item) -> priority(item.source()))
                .thenComparing(MarketItem::addedAt)
                .thenComparing(item -> item.food().key()))
        .toList();
  }

  private static int priority(MarketSource source) {
    return switch (source) {
      case PLAN, RECIPE -> 0;
      case AGENT_CONFIRMED -> 1;
      case MANUAL -> 2;
    };
  }

  private static Optional<CheaperOption> cheaper(
      MarketItem item,
      BigDecimal cost,
      Map<String, BigDecimal> pricePerKg,
      List<SubstitutionRule> rules) {
    return rules.stream()
        .filter(rule -> rule.original().key().equals(item.food().key()))
        .filter(rule -> rule.maxReplaced().compareTo(item.grams()) >= 0)
        .map(
            rule -> {
              Grams grams = item.grams().times(rule.ratio());
              return cost(rule.substitute().key(), grams, pricePerKg)
                  .map(price -> new CheaperOption(rule.substitute(), grams, price));
            })
        .flatMap(Optional::stream)
        .filter(option -> option.estimatedCost().compareTo(cost) < 0)
        .min(Comparator.comparing(CheaperOption::estimatedCost));
  }

  private static Optional<BigDecimal> cost(
      String food, Grams grams, Map<String, BigDecimal> pricePerKg) {
    return Optional.ofNullable(pricePerKg.get(food))
        .map(price -> grams.value().multiply(price).divide(KILO, 2, RoundingMode.HALF_UP));
  }

  private static BigDecimal scaled(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
