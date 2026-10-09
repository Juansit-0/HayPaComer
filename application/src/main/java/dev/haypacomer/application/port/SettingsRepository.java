package dev.haypacomer.application.port;

import dev.haypacomer.application.settings.SettingDefinition;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SettingsRepository {

  List<SettingDefinition> definitions();

  Map<String, String> overrides(HouseholdId household);

  void override(HouseholdId household, String key, String value);

  void reset(HouseholdId household, String key);

  Optional<HouseholdId> householdOf(FridgeId fridge);
}
