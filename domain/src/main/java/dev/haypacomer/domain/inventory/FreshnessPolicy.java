package dev.haypacomer.domain.inventory;

import java.time.LocalDate;
import java.util.Objects;

public record FreshnessPolicy(int atRiskDays) {

  public static final FreshnessPolicy DEFAULT = new FreshnessPolicy(2);

  public FreshnessPolicy {
    if (atRiskDays < 0) {
      throw new IllegalArgumentException("At-risk window cannot be negative: " + atRiskDays);
    }
  }

  public StockedFood apply(StockedFood food, LocalDate today) {
    Objects.requireNonNull(today, "today");
    return food.item().expiresOn().map(expiry -> decorate(food, expiry, today)).orElse(food);
  }

  private StockedFood decorate(StockedFood food, LocalDate expiry, LocalDate today) {
    if (expiry.isBefore(today)) {
      return new ExpiredFood(food);
    }
    if (!expiry.isAfter(today.plusDays(atRiskDays))) {
      return new AtRiskFood(food);
    }
    return food;
  }
}
