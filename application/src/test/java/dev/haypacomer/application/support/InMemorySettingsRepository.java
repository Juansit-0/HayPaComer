package dev.haypacomer.application.support;

import dev.haypacomer.application.port.SettingsRepository;
import dev.haypacomer.application.settings.SettingDefinition;
import dev.haypacomer.application.settings.SettingKind;
import dev.haypacomer.application.settings.SettingScope;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemorySettingsRepository implements SettingsRepository {

  private final List<SettingDefinition> definitions =
      List.of(
          household("fridge.door-alert-seconds", SettingKind.INTEGER, "40", "5", "600"),
          household("fridge.max-celsius", SettingKind.DECIMAL, "5.0", "0", "10"),
          household("fridge.cold-chain-grace-minutes", SettingKind.INTEGER, "20", "1", "240"),
          household("scale.minimum-change-grams", SettingKind.INTEGER, "5", "1", "100"),
          household("food.at-risk-days", SettingKind.INTEGER, "2", "0", "14"),
          household("briefing.morning-hour", SettingKind.INTEGER, "7", "0", "23"),
          household("briefing.digest-hour", SettingKind.INTEGER, "8", "0", "23"),
          household("briefing.expiry-cluster-size", SettingKind.INTEGER, "3", "2", "20"),
          global("cold.discard-after-minutes", "120", "30", "480"),
          global("cold.use-today-after-minutes", "30", "5", "240"),
          global("ai.calls-per-minute", "20", "1", "200"),
          global("ai.circuit-failures", "3", "1", "20"),
          global("ai.circuit-open-seconds", "60", "10", "3600"),
          global("auth.refresh-days", "30", "1", "180"),
          global("auth.max-failed-logins", "5", "3", "20"),
          global("auth.lockout-minutes", "15", "1", "1440"),
          global("auth.min-password-length", "10", "8", "64"));
  private final Map<HouseholdId, Map<String, String>> overrides = new HashMap<>();
  private final Map<FridgeId, HouseholdId> fridges = new HashMap<>();

  public void place(FridgeId fridge, HouseholdId household) {
    fridges.put(fridge, household);
  }

  @Override
  public List<SettingDefinition> definitions() {
    return definitions;
  }

  @Override
  public Map<String, String> overrides(HouseholdId household) {
    return Map.copyOf(overrides.getOrDefault(household, Map.of()));
  }

  @Override
  public void override(HouseholdId household, String key, String value) {
    overrides.computeIfAbsent(household, id -> new HashMap<>()).put(key, value);
  }

  @Override
  public void reset(HouseholdId household, String key) {
    overrides.getOrDefault(household, new HashMap<>()).remove(key);
  }

  @Override
  public Optional<HouseholdId> householdOf(FridgeId fridge) {
    return Optional.ofNullable(fridges.get(fridge));
  }

  private static SettingDefinition household(
      String key, SettingKind kind, String value, String min, String max) {
    return new SettingDefinition(
        key,
        kind,
        SettingScope.HOUSEHOLD,
        value,
        new BigDecimal(min),
        new BigDecimal(max),
        "Household " + key);
  }

  private static SettingDefinition global(String key, String value, String min, String max) {
    return new SettingDefinition(
        key,
        SettingKind.INTEGER,
        SettingScope.GLOBAL,
        value,
        new BigDecimal(min),
        new BigDecimal(max),
        "Global " + key);
  }
}
