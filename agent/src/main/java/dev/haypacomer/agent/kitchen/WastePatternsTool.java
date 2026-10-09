package dev.haypacomer.agent.kitchen;

import dev.haypacomer.agent.runtime.AgentTool;
import dev.haypacomer.agent.runtime.Observation;
import dev.haypacomer.agent.runtime.ToolInvocation;
import dev.haypacomer.agent.tools.ParameterSpec;
import dev.haypacomer.agent.tools.ParameterType;
import dev.haypacomer.agent.tools.ToolSpec;
import dev.haypacomer.application.analytics.FindWastePatterns;
import dev.haypacomer.domain.analytics.WasteTip;
import java.util.List;
import java.util.stream.Collectors;

public final class WastePatternsTool implements AgentTool {

  public static final ToolSpec SPEC =
      ToolSpec.read(
          "waste_patterns",
          "Foods the household threw away at least twice recently, with how much less to buy.",
          List.of(ParameterSpec.optional("days", ParameterType.COUNT, "Days back, default 30")));

  private final FindWastePatterns patterns;

  public WastePatternsTool(FindWastePatterns patterns) {
    this.patterns = patterns;
  }

  @Override
  public ToolSpec spec() {
    return SPEC;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return "Look for waste patterns";
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    int days = Integer.parseInt(invocation.arguments().getOrDefault("days", "30").strip());
    List<WasteTip> tips = patterns.find(invocation.user(), invocation.household(), days);
    if (tips.isEmpty()) {
      return Observation.of(
          SPEC.name(), "No food was thrown away twice in the last " + days + " days");
    }
    return Observation.of(
        SPEC.name(), tips.stream().map(WasteTip::text).collect(Collectors.joining("\n")));
  }
}
