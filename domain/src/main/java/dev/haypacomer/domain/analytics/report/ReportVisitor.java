package dev.haypacomer.domain.analytics.report;

public interface ReportVisitor<R> {

  R visitSummary(ReportSection.Summary summary);

  R visitFoods(ReportSection.Foods foods);

  R visitMembers(ReportSection.Members members);

  R visitTrend(ReportSection.Trend trend);
}
