package dev.haypacomer.domain.analytics.report;

public abstract class ReportTemplate implements ReportVisitor<String> {

  private ReportContext context;

  public final String render(ReportContext report) {
    this.context = report;
    StringBuilder out = new StringBuilder(header(report));
    for (ReportSection section : report.sections()) {
      out.append(section.accept(this));
    }
    out.append(footer(report));
    return out.toString();
  }

  protected final ReportContext context() {
    return context;
  }

  public abstract String mediaType();

  public abstract String extension();

  protected abstract String header(ReportContext report);

  protected String footer(ReportContext report) {
    return "";
  }
}
