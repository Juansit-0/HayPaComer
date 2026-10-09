package dev.haypacomer.application.settings;

import static dev.haypacomer.application.settings.SettingKeys.AI_CALLS_PER_MINUTE;
import static dev.haypacomer.application.settings.SettingKeys.AT_RISK_DAYS;
import static dev.haypacomer.application.settings.SettingKeys.CIRCUIT_FAILURES;
import static dev.haypacomer.application.settings.SettingKeys.CIRCUIT_OPEN_SECONDS;
import static dev.haypacomer.application.settings.SettingKeys.COLD_CHAIN_GRACE_MINUTES;
import static dev.haypacomer.application.settings.SettingKeys.DIGEST_HOUR;
import static dev.haypacomer.application.settings.SettingKeys.DISCARD_AFTER_MINUTES;
import static dev.haypacomer.application.settings.SettingKeys.DOOR_ALERT_SECONDS;
import static dev.haypacomer.application.settings.SettingKeys.EXPIRY_CLUSTER_SIZE;
import static dev.haypacomer.application.settings.SettingKeys.EXPIRY_MARGIN_DAYS;
import static dev.haypacomer.application.settings.SettingKeys.LOCKOUT_MINUTES;
import static dev.haypacomer.application.settings.SettingKeys.MAX_CELSIUS;
import static dev.haypacomer.application.settings.SettingKeys.MAX_FAILED_LOGINS;
import static dev.haypacomer.application.settings.SettingKeys.MINIMUM_CHANGE_GRAMS;
import static dev.haypacomer.application.settings.SettingKeys.MIN_PASSWORD_LENGTH;
import static dev.haypacomer.application.settings.SettingKeys.MORNING_HOUR;
import static dev.haypacomer.application.settings.SettingKeys.REFRESH_DAYS;
import static dev.haypacomer.application.settings.SettingKeys.USE_TODAY_AFTER_MINUTES;

import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.auth.AuthSettings;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.SettingsRepository;
import dev.haypacomer.domain.coldchain.investigation.ColdRule;
import dev.haypacomer.domain.expiry.ExpiryRules;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record StoredPolicies(SettingsRepository settings) implements PolicySource {

  public StoredPolicies {
    Objects.requireNonNull(settings, "settings");
  }

  public BigDecimal value(String key, HouseholdId household) {
    Map<String, SettingDefinition> definitions =
        settings.definitions().stream()
            .collect(Collectors.toMap(SettingDefinition::key, definition -> definition));
    SettingDefinition definition = definitions.get(key);
    if (definition == null) {
      throw new IllegalStateException("Setting missing in the database: " + key);
    }
    String raw = definition.value();
    if (household != null && definition.scope() == SettingScope.HOUSEHOLD) {
      raw = settings.overrides(household).getOrDefault(key, raw);
    }
    return new BigDecimal(raw.strip());
  }

  private int number(String key, HouseholdId household) {
    return value(key, household).intValueExact();
  }

  @Override
  public FreshnessPolicy freshness(HouseholdId household) {
    return new FreshnessPolicy(number(AT_RISK_DAYS, household));
  }

  @Override
  public ExpiryRules expiryRules(HouseholdId household) {
    return new ExpiryRules(number(EXPIRY_MARGIN_DAYS, household));
  }

  @Override
  public FridgeThresholds thresholds(HouseholdId household) {
    return new FridgeThresholds(
        Duration.ofSeconds(number(DOOR_ALERT_SECONDS, household)),
        value(MAX_CELSIUS, household),
        Duration.ofMinutes(number(COLD_CHAIN_GRACE_MINUTES, household)),
        value(MINIMUM_CHANGE_GRAMS, household));
  }

  @Override
  public FridgeThresholds thresholdsOf(FridgeId fridge) {
    return thresholds(settings.householdOf(fridge).orElse(null));
  }

  @Override
  public BriefingSchedule briefings(HouseholdId household) {
    return new BriefingSchedule(
        number(MORNING_HOUR, household),
        number(DIGEST_HOUR, household),
        number(EXPIRY_CLUSTER_SIZE, household));
  }

  @Override
  public ColdRule coldRule() {
    return new ColdRule(
        Duration.ofMinutes(number(DISCARD_AFTER_MINUTES, null)),
        Duration.ofMinutes(number(USE_TODAY_AFTER_MINUTES, null)));
  }

  @Override
  public CircuitPolicy circuit() {
    return new CircuitPolicy(
        number(CIRCUIT_FAILURES, null), Duration.ofSeconds(number(CIRCUIT_OPEN_SECONDS, null)));
  }

  @Override
  public AuthSettings auth() {
    return new AuthSettings(
        Duration.ofDays(number(REFRESH_DAYS, null)),
        number(MAX_FAILED_LOGINS, null),
        Duration.ofMinutes(number(LOCKOUT_MINUTES, null)),
        number(MIN_PASSWORD_LENGTH, null));
  }

  @Override
  public int aiCallsPerMinute() {
    return number(AI_CALLS_PER_MINUTE, null);
  }
}
