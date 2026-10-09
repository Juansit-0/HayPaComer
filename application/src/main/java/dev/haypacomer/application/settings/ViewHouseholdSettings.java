package dev.haypacomer.application.settings;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.SettingsRepository;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ViewHouseholdSettings {

  private final GetHousehold households;
  private final SettingsRepository settings;

  public ViewHouseholdSettings(HouseholdRepository households, SettingsRepository settings) {
    this.households = new GetHousehold(households);
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  public List<SettingView> view(UserId actor, HouseholdId household) {
    households.get(actor, household);
    Map<String, String> overrides = settings.overrides(household);
    return settings.definitions().stream()
        .filter(definition -> definition.scope() == SettingScope.HOUSEHOLD)
        .map(
            definition ->
                new SettingView(
                    definition,
                    overrides.getOrDefault(definition.key(), definition.value()),
                    overrides.containsKey(definition.key())))
        .toList();
  }
}
