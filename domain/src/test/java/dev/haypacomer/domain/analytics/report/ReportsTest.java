package dev.haypacomer.domain.analytics.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.analytics.DayTally;
import dev.haypacomer.domain.analytics.FoodTally;
import dev.haypacomer.domain.analytics.HouseholdMetrics;
import dev.haypacomer.domain.analytics.MemberTally;
import dev.haypacomer.domain.analytics.Tally;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReportsTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
  private static final UserId JUAN = UserId.newId();
  private static final UserId GONE = UserId.newId();
  private static final Tally CHICKEN = new Tally(Grams.of(600), Grams.of(600), Grams.ZERO);
  private static final Tally RICE = new Tally(Grams.of(300), Grams.ZERO, Grams.of(1000));
  private static final Tally TOTAL = CHICKEN.plus(RICE);

  private static ReportContext context(String name) {
    HouseholdMetrics metrics =
        new HouseholdMetrics(
            MONDAY,
            MONDAY.plusDays(1),
            TOTAL,
            new BigDecimal("13200.00"),
            new BigDecimal("4800.00"),
            List.of(new FoodTally("chicken breast", CHICKEN), new FoodTally("rice, white", RICE)),
            List.of(new MemberTally(JUAN, CHICKEN), new MemberTally(GONE, RICE)),
            List.of(new DayTally(MONDAY, TOTAL), new DayTally(MONDAY.plusDays(1), Tally.EMPTY)),
            Set.of("mystery"));
    return new ReportContext(name, Currency.getInstance("COP"), metrics, Map.of(JUAN, "=Juan"));
  }

  @Test
  void csvWalksEverySectionInOrderAndEscapesCells() {
    CsvReport csv = new CsvReport();

    String report = csv.render(context("Apartment"));

    List<String> lines = List.of(report.split("\n"));
    assertEquals("section,key,consumed_g,rescued_g,discarded_g,waste_rate", lines.getFirst());
    assertEquals("total,all,900,600,1000,0.526", lines.get(1));
    assertEquals("money,saved_COP,13200.00,,,", lines.get(2));
    assertEquals("food,chicken breast,600,600,0,0.000", lines.get(4));
    assertEquals("food,\"rice, white\",300,0,1000,0.769", lines.get(5));
    assertEquals("member,'=Juan,600,600,0,0.000", lines.get(6));
    assertEquals("member,Former member,300,0,1000,0.769", lines.get(7));
    assertEquals("day,2026-10-06,0,0,0,0.000", lines.getLast());
    assertEquals("text/csv", csv.mediaType());
    assertEquals("csv", csv.extension());
    assertEquals("\"a\"\"b\"", CsvReport.escape("a\"b"));
  }

  @Test
  void markdownReadsLikeAReportForPeople() {
    MarkdownReport markdown = new MarkdownReport();

    String report = markdown.render(context("Apart|ment #1"));

    assertTrue(report.startsWith("# Apart/ment 1 kitchen report\n\n2026-10-05 to 2026-10-06"));
    assertTrue(report.contains("- Rescued before expiring: 0.60 kg"));
    assertTrue(report.contains("(52.6% of what left the fridge)"));
    assertTrue(report.contains("- Money saved: 13200.00 COP"));
    assertTrue(report.contains("- Foods without a price: mystery"));
    assertTrue(report.contains("| rice, white | 0.30 kg | 0.00 kg | 1.00 kg |"));
    assertTrue(report.contains("| =Juan | 0.60 kg | 0.60 kg | 0.00 kg |"));
    assertTrue(report.contains("- 2026-10-05: 0.90 kg eaten, 1.00 kg thrown away"));
    assertTrue(!report.contains("2026-10-06: "));
    assertTrue(report.endsWith("of each food.\n"));
    assertEquals("text/markdown", markdown.mediaType());
    assertEquals("md", markdown.extension());
  }

  @Test
  void emptyPeriodsSaySoInsteadOfShowingEmptyTables() {
    HouseholdMetrics empty =
        new HouseholdMetrics(
            MONDAY,
            MONDAY,
            Tally.EMPTY,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(),
            List.of(),
            List.of(new DayTally(MONDAY, Tally.EMPTY)),
            Set.of());

    String report =
        new MarkdownReport()
            .render(new ReportContext("Home", Currency.getInstance("USD"), empty, Map.of()));

    assertTrue(report.contains("No food left the fridge in this period."));
    assertTrue(!report.contains("## Members"));
    assertTrue(!report.contains("Foods without a price"));
  }

  @Test
  void anyVisitorCanWalkTheSameSections() {
    List<String> visited = new ArrayList<>();
    ReportVisitor<Integer> counter =
        new ReportVisitor<>() {
          @Override
          public Integer visitSummary(ReportSection.Summary summary) {
            visited.add("summary");
            return 1;
          }

          @Override
          public Integer visitFoods(ReportSection.Foods foods) {
            visited.add("foods");
            return foods.foods().size();
          }

          @Override
          public Integer visitMembers(ReportSection.Members members) {
            visited.add("members");
            return members.members().size();
          }

          @Override
          public Integer visitTrend(ReportSection.Trend trend) {
            visited.add("trend");
            return trend.days().size();
          }
        };

    int total =
        context("Home").sections().stream().mapToInt(section -> section.accept(counter)).sum();

    assertEquals(List.of("summary", "foods", "members", "trend"), visited);
    assertEquals(7, total);
  }
}
