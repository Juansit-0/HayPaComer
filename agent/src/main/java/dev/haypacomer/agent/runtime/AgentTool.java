package dev.haypacomer.agent.runtime;

public interface AgentTool {

  String name();

  ToolKind kind();

  String describe(ToolInvocation invocation);

  Observation invoke(ToolInvocation invocation);
}
