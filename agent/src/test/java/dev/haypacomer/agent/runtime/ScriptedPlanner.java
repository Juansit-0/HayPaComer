package dev.haypacomer.agent.runtime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

final class ScriptedPlanner implements Planner {

  private final Deque<Function<AgentContext, Decision>> script;
  final List<AgentContext> seen = new ArrayList<>();

  @SafeVarargs
  ScriptedPlanner(Function<AgentContext, Decision>... steps) {
    this.script = new ArrayDeque<>(List.of(steps));
  }

  @Override
  public String name() {
    return "scripted";
  }

  @Override
  public Decision next(AgentContext context) {
    seen.add(context);
    if (script.isEmpty()) {
      throw new PlannerUnavailableException("script ended");
    }
    return script.poll().apply(context);
  }
}
