package dev.haypacomer.domain.analytics.report;

import dev.haypacomer.domain.analytics.HouseholdMetrics;
import dev.haypacomer.domain.identity.UserId;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ReportContext(
    String householdName, Currency currency, HouseholdMetrics metrics, Map<UserId, String> names) {

  public ReportContext {
    Objects.requireNonNull(householdName, "householdName");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(metrics, "metrics");
    names = Map.copyOf(names);
  }

  public List<ReportSection> sections() {
    return List.of(
        new ReportSection.Summary(
            metrics.total(), metrics.moneySaved(), metrics.moneyWasted(), metrics.unpriced()),
        new ReportSection.Foods(metrics.foods()),
        new ReportSection.Members(metrics.members()),
        new ReportSection.Trend(metrics.days()));
  }

  public String nameOf(UserId user) {
    return names.getOrDefault(user, "Former member");
  }
}
