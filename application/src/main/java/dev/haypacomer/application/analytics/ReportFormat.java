package dev.haypacomer.application.analytics;

import dev.haypacomer.domain.analytics.report.CsvReport;
import dev.haypacomer.domain.analytics.report.MarkdownReport;
import dev.haypacomer.domain.analytics.report.ReportTemplate;
import java.util.function.Supplier;

public enum ReportFormat {
  CSV(CsvReport::new),
  MARKDOWN(MarkdownReport::new);

  private final Supplier<ReportTemplate> template;

  ReportFormat(Supplier<ReportTemplate> template) {
    this.template = template;
  }

  ReportTemplate template() {
    return template.get();
  }
}
