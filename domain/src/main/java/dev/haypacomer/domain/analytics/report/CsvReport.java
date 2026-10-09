package dev.haypacomer.domain.analytics.report;

import dev.haypacomer.domain.analytics.Tally;
import dev.haypacomer.domain.quantity.Grams;
import java.util.stream.Collectors;

public final class CsvReport extends ReportTemplate {

  @Override
  public String mediaType() {
    return "text/csv";
  }

  @Override
  public String extension() {
    return "csv";
  }

  @Override
  protected String header(ReportContext report) {
    return "section,key,consumed_g,rescued_g,discarded_g,waste_rate\n";
  }

  @Override
  public String visitSummary(ReportSection.Summary summary) {
    return row("total", "all", summary.total())
        + "money,saved_"
        + context().currency().getCurrencyCode()
        + ","
        + summary.moneySaved().toPlainString()
        + ",,,\n"
        + "money,wasted_"
        + context().currency().getCurrencyCode()
        + ","
        + summary.moneyWasted().toPlainString()
        + ",,,\n";
  }

  @Override
  public String visitFoods(ReportSection.Foods foods) {
    return foods.foods().stream()
        .map(food -> row("food", food.foodKey(), food.tally()))
        .collect(Collectors.joining());
  }

  @Override
  public String visitMembers(ReportSection.Members members) {
    return members.members().stream()
        .map(member -> row("member", context().nameOf(member.user()), member.tally()))
        .collect(Collectors.joining());
  }

  @Override
  public String visitTrend(ReportSection.Trend trend) {
    return trend.days().stream()
        .map(day -> row("day", day.day().toString(), day.tally()))
        .collect(Collectors.joining());
  }

  private static String row(String section, String key, Tally tally) {
    return section
        + ","
        + escape(key)
        + ","
        + plain(tally.consumed())
        + ","
        + plain(tally.rescued())
        + ","
        + plain(tally.discarded())
        + ","
        + String.format(java.util.Locale.ROOT, "%.3f", tally.wasteRate())
        + "\n";
  }

  private static String plain(Grams grams) {
    return grams.value().stripTrailingZeros().toPlainString();
  }

  static String escape(String value) {
    String safe = value.matches("^[=+\\-@].*") ? "'" + value : value;
    if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
      return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    return safe;
  }
}
