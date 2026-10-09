package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.planning.ViewCurrentPlan;
import dev.haypacomer.application.planning.WeeklyPlanNotFoundException;
import dev.haypacomer.domain.planning.WeeklyPlan;
import java.util.List;
import java.util.stream.Collectors;

public final class ViewWeeklyPlanTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read("view_weekly_plan", "This week's meal plan, day by day.", List.of());

  private final ViewCurrentPlan plans;

  public ViewWeeklyPlanTool(ViewCurrentPlan plans) {
    this.plans = plans;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Read the weekly plan";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    WeeklyPlan plan;
    try {
      plan = plans.view(invocation.user(), invocation.household());
    } catch (WeeklyPlanNotFoundException none) {
      return Observation.of(SPEC.name(), "No plan for this week");
    }
    return Observation.of(
        SPEC.name(),
        "Week of "
            + plan.weekStart()
            + "\n"
            + plan.entries().stream()
                .map(
                    entry ->
                        plan.dateOf(entry)
                            + " "
                            + entry.meal()
                            + ": "
                            + entry.recipe().name()
                            + " for "
                            + entry.servings()
                            + (entry.needsShopping() ? ", needs shopping" : ""))
                .collect(Collectors.joining("\n")));
  }
}
