package dev.haypacomer.domain.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record HouseholdMetrics(
    LocalDate from,
    LocalDate to,
    Tally total,
    BigDecimal moneySaved,
    BigDecimal moneyWasted,
    List<FoodTally> foods,
    List<MemberTally> members,
    List<DayTally> days,
    Set<String> unpriced) {

  public HouseholdMetrics {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(total, "total");
    Objects.requireNonNull(moneySaved, "moneySaved");
    Objects.requireNonNull(moneyWasted, "moneyWasted");
    foods = List.copyOf(foods);
    members = List.copyOf(members);
    days = List.copyOf(days);
    unpriced = Set.copyOf(unpriced);
  }
}
