package dev.haypacomer.agent.runtime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RuleBasedPlanner implements Planner {

  private final List<String> reads;

  public RuleBasedPlanner(List<String> reads) {
    this.reads = List.copyOf(reads);
  }

  @Override
  public String name() {
    return "offline";
  }

  @Override
  public Decision next(AgentContext context) {
    for (String tool : reads) {
      if (context.tools().contains(tool) && !context.called(tool) && context.stepsLeft() > 0) {
        return new Decision.CallTool(tool, Map.of(), "Offline rules read " + tool);
      }
    }
    String evidence =
        context.history().stream()
            .map(Exchange::observation)
            .filter(observation -> !observation.failed())
            .map(observation -> observation.tool() + ": " + observation.content())
            .collect(Collectors.joining("\n"));
    if (evidence.isEmpty()) {
      return new Decision.FinalAnswer("No evidence available right now; nothing was changed.");
    }
    return new Decision.FinalAnswer("Offline answer based on measured data:\n" + evidence);
  }
}
