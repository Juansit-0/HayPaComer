package dev.haypacomer.agent.runtime;

public interface Planner {

  String name();

  Decision next(AgentContext context);
}
