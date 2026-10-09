package dev.haypacomer.agent.runtime;

import dev.haypacomer.agent.tools.ToolSpec;
import java.util.ArrayList;
import java.util.List;

final class FakeTool implements AgentTool {

  private final ToolSpec spec;
  private final String result;
  final List<ToolInvocation> invocations = new ArrayList<>();

  FakeTool(ToolSpec spec, String result) {
    this.spec = spec;
    this.result = result;
  }

  @Override
  public ToolSpec spec() {
    return spec;
  }

  @Override
  public String describe(ToolInvocation invocation) {
    return name() + " " + invocation.arguments();
  }

  @Override
  public Observation invoke(ToolInvocation invocation) {
    invocations.add(invocation);
    if (result == null) {
      throw new IllegalStateException("scale offline");
    }
    return Observation.of(name(), result);
  }
}
