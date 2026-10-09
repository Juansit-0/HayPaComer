package dev.haypacomer.agent.tools;

import dev.haypacomer.agent.runtime.AgentTool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ToolRegistry {

  private final Map<String, AgentTool> tools;
  private final List<String> order;

  public ToolRegistry(List<AgentTool> tools) {
    Map<String, AgentTool> byName = new LinkedHashMap<>();
    for (AgentTool tool : tools) {
      if (byName.putIfAbsent(tool.name(), tool) != null) {
        throw new IllegalArgumentException("Tool registered twice: " + tool.name());
      }
    }
    this.tools = Map.copyOf(byName);
    this.order = List.copyOf(byName.keySet());
  }

  public Optional<AgentTool> find(String name) {
    return Optional.ofNullable(tools.get(name));
  }

  public Set<String> names() {
    return tools.keySet();
  }

  public List<ToolSpec> specs() {
    return order.stream().map(name -> tools.get(name).spec()).toList();
  }

  public ToolRegistry allow(Set<String> allowlist) {
    for (String name : allowlist) {
      if (!tools.containsKey(name)) {
        throw new IllegalArgumentException("Allowlist names an unknown tool: " + name);
      }
    }
    return new ToolRegistry(order.stream().filter(allowlist::contains).map(tools::get).toList());
  }
}
