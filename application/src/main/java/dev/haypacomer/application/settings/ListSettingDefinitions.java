package dev.haypacomer.application.settings;

import dev.haypacomer.application.port.SettingsRepository;
import java.util.List;
import java.util.Objects;

public final class ListSettingDefinitions {

  private final SettingsRepository settings;

  public ListSettingDefinitions(SettingsRepository settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  public List<SettingDefinition> list() {
    return settings.definitions();
  }
}
