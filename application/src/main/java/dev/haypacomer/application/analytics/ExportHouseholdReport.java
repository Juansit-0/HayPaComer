package dev.haypacomer.application.analytics;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.analytics.report.ReportContext;
import dev.haypacomer.domain.analytics.report.ReportTemplate;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

public final class ExportHouseholdReport {

  private final GetHousehold households;
  private final HouseholdMemberNames names;
  private final ViewHouseholdMetrics metrics;

  public ExportHouseholdReport(
      HouseholdRepository households, UserRepository users, ViewHouseholdMetrics metrics) {
    this.households = new GetHousehold(households);
    this.names = new HouseholdMemberNames(households, users);
    this.metrics = Objects.requireNonNull(metrics, "metrics");
  }

  public ExportedReport export(
      UserId actor, HouseholdId householdId, LocalDate from, LocalDate to, ReportFormat format) {
    Household household = households.get(actor, householdId);
    MetricsReport report = metrics.view(actor, householdId, from, to);
    Map<UserId, String> members = names.names(actor, householdId);
    ReportTemplate template = format.template();
    String content =
        template.render(
            new ReportContext(household.name(), report.currency(), report.metrics(), members));
    return new ExportedReport(
        "haypacomer-" + from + "-" + to + "." + template.extension(),
        template.mediaType(),
        content);
  }
}
