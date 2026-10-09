package dev.haypacomer.agent.runtime;

import dev.haypacomer.agent.tools.ToolSpec;
import java.util.Optional;

public interface AgentTool {

  ToolSpec spec();

  String describe(ToolInvocation invocation);

  Observation invoke(ToolInvocation invocation);

  default Optional<String> problem(ToolInvocation invocation) {
    return Optional.empty();
  }

  default String name() {
    return spec().name();
  }

  default ToolKind kind() {
    return spec().kind();
  }
}
