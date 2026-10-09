package dev.haypacomer.agent.tools;

import dev.haypacomer.agent.runtime.ToolInvocation;
import java.util.Map;
import java.util.Optional;

public final class SchemaGuardrail implements Guardrail {

  @Override
  public Optional<String> reject(ToolSpec spec, ToolInvocation invocation) {
    for (String name : invocation.arguments().keySet()) {
      if (spec.parameter(name).isEmpty()) {
        return Optional.of("Unknown argument " + name);
      }
    }
    Map<String, String> arguments = invocation.arguments();
    for (ParameterSpec parameter : spec.parameters()) {
      String value = arguments.get(parameter.name());
      if (value == null) {
        if (parameter.required()) {
          return Optional.of("Missing argument " + parameter.name());
        }
        continue;
      }
      Optional<String> problem = parameter.type().problem(value.strip());
      if (problem.isPresent()) {
        return Optional.of("Argument " + parameter.name() + " " + problem.get());
      }
    }
    return Optional.empty();
  }
}
