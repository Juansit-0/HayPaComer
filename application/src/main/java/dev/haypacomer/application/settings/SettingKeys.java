package dev.haypacomer.application.settings;

public final class SettingKeys {

  public static final String DOOR_ALERT_SECONDS = "fridge.door-alert-seconds";
  public static final String MAX_CELSIUS = "fridge.max-celsius";
  public static final String COLD_CHAIN_GRACE_MINUTES = "fridge.cold-chain-grace-minutes";
  public static final String MINIMUM_CHANGE_GRAMS = "scale.minimum-change-grams";
  public static final String AT_RISK_DAYS = "food.at-risk-days";
  public static final String EXPIRY_MARGIN_DAYS = "food.expiry-margin-days";
  public static final String MORNING_HOUR = "briefing.morning-hour";
  public static final String DIGEST_HOUR = "briefing.digest-hour";
  public static final String EXPIRY_CLUSTER_SIZE = "briefing.expiry-cluster-size";
  public static final String DISCARD_AFTER_MINUTES = "cold.discard-after-minutes";
  public static final String USE_TODAY_AFTER_MINUTES = "cold.use-today-after-minutes";
  public static final String AI_CALLS_PER_MINUTE = "ai.calls-per-minute";
  public static final String CIRCUIT_FAILURES = "ai.circuit-failures";
  public static final String CIRCUIT_OPEN_SECONDS = "ai.circuit-open-seconds";
  public static final String REFRESH_DAYS = "auth.refresh-days";
  public static final String MAX_FAILED_LOGINS = "auth.max-failed-logins";
  public static final String LOCKOUT_MINUTES = "auth.lockout-minutes";
  public static final String MIN_PASSWORD_LENGTH = "auth.min-password-length";

  private SettingKeys() {}
}
