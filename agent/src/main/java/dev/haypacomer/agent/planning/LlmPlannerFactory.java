package dev.haypacomer.agent.planning;

import dev.haypacomer.agent.runtime.Planner;
import dev.haypacomer.agent.supervisor.PlannerFactory;
import dev.haypacomer.agent.supervisor.Specialist;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.application.port.ChatModel;

public final class LlmPlannerFactory implements PlannerFactory {

  private final ChatModel model;
  private final ToolRegistry tools;

  public LlmPlannerFactory(ChatModel model, ToolRegistry tools) {
    this.model = model;
    this.tools = tools;
  }

  @Override
  public Planner plannerFor(Specialist specialist) {
    return new LlmPlanner(model, specialist, tools);
  }
}
