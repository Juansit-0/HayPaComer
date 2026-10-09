package dev.haypacomer.domain.analytics.report;

import dev.haypacomer.domain.analytics.Tally;
import dev.haypacomer.domain.quantity.Grams;
import java.util.Locale;
import java.util.stream.Collectors;

public final class MarkdownReport extends ReportTemplate {

  @Override
  public String mediaType() {
    return "text/markdown";
  }

  @Override
  public String extension() {
    return "md";
  }

  @Override
  protected String header(ReportContext report) {
    return "# "
        + clean(report.householdName())
        + " kitchen report\n\n"
        + report.metrics().from()
        + " to "
        + report.metrics().to()
        + "\n\n";
  }

  @Override
  protected String footer(ReportContext report) {
    return "\nGrams come from measured inventory movements; money uses the price per kilogram"
        + " of each food.\n";
  }

  @Override
  public String visitSummary(ReportSection.Summary summary) {
    String currency = context().currency().getCurrencyCode();
    String unpriced =
        summary.unpriced().isEmpty()
            ? ""
            : "- Foods without a price: " + String.join(", ", summary.unpriced()) + "\n";
    return "## Summary\n\n"
        + "- Eaten: "
        + kilos(summary.total().consumed())
        + "\n- Rescued before expiring: "
        + kilos(summary.total().rescued())
        + "\n- Thrown away: "
        + kilos(summary.total().discarded())
        + " ("
        + percent(summary.total())
        + " of what left the fridge)\n- Money saved: "
        + summary.moneySaved().toPlainString()
        + " "
        + currency
        + "\n- Money wasted: "
        + summary.moneyWasted().toPlainString()
        + " "
        + currency
        + "\n"
        + unpriced
        + "\n";
  }

  @Override
  public String visitFoods(ReportSection.Foods foods) {
    if (foods.foods().isEmpty()) {
      return "## Foods\n\nNo food left the fridge in this period.\n\n";
    }
    return "## Foods\n\n| Food | Eaten | Rescued | Thrown away |\n|---|---|---|---|\n"
        + foods.foods().stream()
            .map(food -> line(food.foodKey(), food.tally()))
            .collect(Collectors.joining())
        + "\n";
  }

  @Override
  public String visitMembers(ReportSection.Members members) {
    if (members.members().isEmpty()) {
      return "";
    }
    return "## Members\n\n| Member | Eaten | Rescued | Thrown away |\n|---|---|---|---|\n"
        + members.members().stream()
            .map(member -> line(context().nameOf(member.user()), member.tally()))
            .collect(Collectors.joining())
        + "\n";
  }

  @Override
  public String visitTrend(ReportSection.Trend trend) {
    return "## Days with activity\n\n"
        + trend.days().stream()
            .filter(day -> !day.tally().equals(Tally.EMPTY))
            .map(
                day ->
                    "- "
                        + day.day()
                        + ": "
                        + kilos(day.tally().consumed())
                        + " eaten, "
                        + kilos(day.tally().discarded())
                        + " thrown away\n")
            .collect(Collectors.joining());
  }

  private static String line(String name, Tally tally) {
    return "| "
        + clean(name)
        + " | "
        + kilos(tally.consumed())
        + " | "
        + kilos(tally.rescued())
        + " | "
        + kilos(tally.discarded())
        + " |\n";
  }

  private static String kilos(Grams grams) {
    return String.format(Locale.ROOT, "%.2f kg", grams.value().doubleValue() / 1000);
  }

  private static String percent(Tally tally) {
    return String.format(Locale.ROOT, "%.1f%%", tally.wasteRate() * 100);
  }

  static String clean(String text) {
    return text.replace("|", "/").replace("\n", " ").replace("#", "").strip();
  }
}
