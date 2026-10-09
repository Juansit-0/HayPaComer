package dev.haypacomer.application.settings;

import java.util.Objects;

public record SettingView(SettingDefinition definition, String value, boolean overridden) {

  public SettingView {
    Objects.requireNonNull(definition, "definition");
    Objects.requireNonNull(value, "value");
  }
}
