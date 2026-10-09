package dev.haypacomer.application.settings;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.SettingsRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;
import java.util.Optional;

public final class ChangeHouseholdSetting {

  private final GetHousehold households;
  private final SettingsRepository settings;

  public ChangeHouseholdSetting(HouseholdRepository households, SettingsRepository settings) {
    this.households = new GetHousehold(households);
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  public SettingView change(
      UserId actor, HouseholdId household, String key, Optional<String> value) {
    households.get(actor, household).requirePermission(actor, Permission.MANAGE_HOUSEHOLD);
    SettingDefinition definition =
        settings.definitions().stream()
            .filter(candidate -> candidate.key().equals(key))
            .filter(candidate -> candidate.scope() == SettingScope.HOUSEHOLD)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown household setting " + key));
    if (value.isEmpty()) {
      settings.reset(household, key);
      return new SettingView(definition, definition.value(), false);
    }
    String accepted = definition.parse(value.get()).stripTrailingZeros().toPlainString();
    settings.override(household, key, accepted);
    return new SettingView(definition, accepted, true);
  }
}
