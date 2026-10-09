package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.market.BudgetReport;
import dev.haypacomer.application.market.MarketBudgetNotSetException;
import dev.haypacomer.application.market.ViewMarketBudget;
import dev.haypacomer.domain.market.BudgetLine;
import dev.haypacomer.domain.market.BudgetPlan;
import java.util.List;
import java.util.stream.Collectors;

public final class ReviewBudgetTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read(
          "review_budget",
          "This month's market budget: spent, left, what fits in priority order, what does not,"
              + " and cheaper allowed substitutes.",
          List.of());

  private final ViewMarketBudget budget;

  public ReviewBudgetTool(ViewMarketBudget budget) {
    this.budget = budget;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Review the market budget";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    BudgetReport report;
    try {
      report = budget.view(invocation.user(), invocation.household());
    } catch (MarketBudgetNotSetException none) {
      return Observation.of(SPEC.name(), "No monthly market budget yet");
    }
    BudgetPlan plan = report.plan();
    String currency = report.currency().getCurrencyCode();
    StringBuilder text =
        new StringBuilder("Budget ")
            .append(plan.monthly().toPlainString())
            .append(' ')
            .append(currency)
            .append(", spent ")
            .append(plan.spent().toPlainString())
            .append(", left ")
            .append(plan.remaining().toPlainString())
            .append(", the list in budget costs ")
            .append(plan.plannedCost().toPlainString());
    if (!plan.overBudget().isEmpty()) {
      text.append("\nDoes not fit: ")
          .append(
              plan.overBudget().stream()
                  .map(ReviewBudgetTool::line)
                  .collect(Collectors.joining("; ")));
    }
    if (!plan.unpriced().isEmpty()) {
      text.append("\nNo price yet: ")
          .append(
              plan.unpriced().stream()
                  .map(line -> line.item().food().name())
                  .collect(Collectors.joining(", ")));
    }
    return Observation.of(SPEC.name(), text.toString());
  }

  private static String line(BudgetLine line) {
    String base =
        line.item().food().name()
            + " "
            + line.item().grams()
            + " for "
            + line.cost().orElseThrow().toPlainString();
    return line.cheaperOption()
        .map(
            option ->
                base
                    + " (cheaper: "
                    + option.substitute().name()
                    + " "
                    + option.grams()
                    + " for "
                    + option.estimatedCost().toPlainString()
                    + ")")
        .orElse(base);
  }
}
