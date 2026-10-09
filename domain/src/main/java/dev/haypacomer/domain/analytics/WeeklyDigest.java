package dev.haypacomer.domain.analytics;

import dev.haypacomer.domain.identity.UserId;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record WeeklyDigest(
    HouseholdMetrics thisWeek,
    HouseholdMetrics lastWeek,
    List<WasteTip> tips,
    Currency currency,
    Map<UserId, String> names) {

  public WeeklyDigest {
    Objects.requireNonNull(thisWeek, "thisWeek");
    Objects.requireNonNull(lastWeek, "lastWeek");
    Objects.requireNonNull(currency, "currency");
    tips = List.copyOf(tips);
    names = Map.copyOf(names);
  }

  public Optional<String> topRescuer() {
    return thisWeek.members().stream()
        .filter(member -> !member.tally().rescued().isZero())
        .findFirst()
        .map(member -> names.getOrDefault(member.user(), "A former member"));
  }

  public String text() {
    Tally now = thisWeek.total();
    Tally before = lastWeek.total();
    StringBuilder text =
        new StringBuilder("This week you rescued ")
            .append(now.rescued())
            .append(" before it expired and saved ")
            .append(thisWeek.moneySaved().toPlainString())
            .append(' ')
            .append(currency.getCurrencyCode())
            .append(". You threw away ")
            .append(now.discarded())
            .append(" (")
            .append(percent(now.wasteRate()))
            .append(" of what left the fridge, ")
            .append(trend(now.wasteRate(), before.wasteRate()))
            .append(").");
    topRescuer().ifPresent(name -> text.append(" Top rescuer: ").append(name).append('.'));
    tips.forEach(tip -> text.append('\n').append(tip.text()));
    if (tips.isEmpty() && !now.discarded().isZero()) {
      text.append("\nNo food was thrown away twice; keep using what expires first.");
    }
    return text.toString();
  }

  private static String percent(double rate) {
    return Math.round(rate * 100) + "%";
  }

  private static String trend(double now, double before) {
    long change = Math.round((now - before) * 100);
    if (change == 0) {
      return "the same as last week";
    }
    return change < 0
        ? -change + " points better than last week"
        : change + " points worse than last week";
  }
}
