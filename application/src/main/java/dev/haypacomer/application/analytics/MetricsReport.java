package dev.haypacomer.application.analytics;

import dev.haypacomer.domain.analytics.HouseholdMetrics;
import java.util.Currency;
import java.util.Objects;

public record MetricsReport(HouseholdMetrics metrics, Currency currency) {

  public MetricsReport {
    Objects.requireNonNull(metrics, "metrics");
    Objects.requireNonNull(currency, "currency");
  }
}
