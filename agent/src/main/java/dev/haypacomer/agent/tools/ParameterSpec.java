package dev.haypacomer.agent.tools;

import java.util.Objects;

public record ParameterSpec(String name, ParameterType type, boolean required, String description) {

  public ParameterSpec {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(description, "description");
    if (!name.matches("[a-z][a-z_]{0,39}")) {
      throw new IllegalArgumentException("Parameter names are lower snake case: " + name);
    }
  }

  public static ParameterSpec required(String name, ParameterType type, String description) {
    return new ParameterSpec(name, type, true, description);
  }

  public static ParameterSpec optional(String name, ParameterType type, String description) {
    return new ParameterSpec(name, type, false, description);
  }
}
