package dev.haypacomer.agent.runtime;

import dev.haypacomer.agent.tools.ToolSpec;

public interface AgentTool {

  ToolSpec spec();

  String describe(ToolInvocation invocation);

  Observation invoke(ToolInvocation invocation);

  default String name() {
    return spec().name();
  }

  default ToolKind kind() {
    return spec().kind();
  }
}
