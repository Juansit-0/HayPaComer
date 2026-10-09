package dev.haypacomer.domain.analytics.report;

import dev.haypacomer.domain.analytics.DayTally;
import dev.haypacomer.domain.analytics.FoodTally;
import dev.haypacomer.domain.analytics.MemberTally;
import dev.haypacomer.domain.analytics.Tally;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public sealed interface ReportSection {

  <R> R accept(ReportVisitor<R> visitor);

  record Summary(Tally total, BigDecimal moneySaved, BigDecimal moneyWasted, Set<String> unpriced)
      implements ReportSection {

    public Summary {
      Objects.requireNonNull(total, "total");
      Objects.requireNonNull(moneySaved, "moneySaved");
      Objects.requireNonNull(moneyWasted, "moneyWasted");
      unpriced = Set.copyOf(unpriced);
    }

    @Override
    public <R> R accept(ReportVisitor<R> visitor) {
      return visitor.visitSummary(this);
    }
  }

  record Foods(List<FoodTally> foods) implements ReportSection {

    public Foods {
      foods = List.copyOf(foods);
    }

    @Override
    public <R> R accept(ReportVisitor<R> visitor) {
      return visitor.visitFoods(this);
    }
  }

  record Members(List<MemberTally> members) implements ReportSection {

    public Members {
      members = List.copyOf(members);
    }

    @Override
    public <R> R accept(ReportVisitor<R> visitor) {
      return visitor.visitMembers(this);
    }
  }

  record Trend(List<DayTally> days) implements ReportSection {

    public Trend {
      days = List.copyOf(days);
    }

    @Override
    public <R> R accept(ReportVisitor<R> visitor) {
      return visitor.visitTrend(this);
    }
  }
}
