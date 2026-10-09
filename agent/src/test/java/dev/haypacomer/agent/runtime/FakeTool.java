package dev.haypacomer.agent.runtime;

import java.util.ArrayList;
import java.util.List;

final class FakeTool implements AgentTool {

  private final String name;
  private final ToolKind kind;
  private final String result;
  final List<ToolInvocation> invocations = new ArrayList<>();

  FakeTool(String name, ToolKind kind, String result) {
    this.name = name;
    this.kind = kind;
    this.result = result;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public ToolKind kind() {
    return kind;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return name + " " + invocation.arguments();
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    invocations.add(invocation);
    if (result == null) {
      throw new IllegalStateException("scale offline");
    }
    return Observation.of(name, result);
  }
}
