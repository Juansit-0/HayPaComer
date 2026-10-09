package dev.haypacomer.agent.tools;

import dev.haypacomer.agent.runtime.ToolKind;
import dev.haypacomer.domain.household.Permission;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ToolSpec(
    String name,
    String description,
    ToolKind kind,
    Permission permission,
    List<ParameterSpec> parameters) {

  public ToolSpec {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(permission, "permission");
    parameters = List.copyOf(parameters);
    if (!name.matches("[a-z][a-z_]{2,39}")) {
      throw new IllegalArgumentException("Tool names are lower snake case: " + name);
    }
    if (kind == ToolKind.WRITE && permission == Permission.VIEW_HOUSEHOLD) {
      throw new IllegalArgumentException("A write tool needs a permission beyond viewing");
    }
    var names = new HashSet<String>();
    for (ParameterSpec parameter : parameters) {
      if (!names.add(parameter.name())) {
        throw new IllegalArgumentException("Repeated parameter " + parameter.name());
      }
    }
  }

  public static ToolSpec read(String name, String description, List<ParameterSpec> parameters) {
    return new ToolSpec(name, description, ToolKind.READ, Permission.VIEW_HOUSEHOLD, parameters);
  }

  public static ToolSpec write(
      String name, String description, Permission permission, List<ParameterSpec> parameters) {
    return new ToolSpec(name, description, ToolKind.WRITE, permission, parameters);
  }

  public Optional<ParameterSpec> parameter(String name) {
    return parameters.stream().filter(parameter -> parameter.name().equals(name)).findFirst();
  }
}
