package dev.haypacomer.application.settings;

import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.auth.AuthSettings;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.domain.coldchain.investigation.ColdRule;
import dev.haypacomer.domain.expiry.ExpiryRules;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.sensor.FridgeThresholds;

public record FixedPolicies(
    FreshnessPolicy freshness,
    FridgeThresholds thresholds,
    BriefingSchedule briefings,
    ColdRule coldRule,
    CircuitPolicy circuit,
    AuthSettings auth,
    int aiCallsPerMinute,
    ExpiryRules expiryRules)
    implements PolicySource {

  public static final FixedPolicies DEFAULT =
      new FixedPolicies(
          FreshnessPolicy.DEFAULT,
          FridgeThresholds.DEFAULT,
          BriefingSchedule.DEFAULT,
          ColdRule.DEFAULT,
          CircuitPolicy.DEFAULT,
          AuthSettings.DEFAULT,
          20,
          ExpiryRules.DEFAULT);

  public FixedPolicies withFreshness(FreshnessPolicy value) {
    return new FixedPolicies(
        value, thresholds, briefings, coldRule, circuit, auth, aiCallsPerMinute, expiryRules);
  }

  public FixedPolicies withThresholds(FridgeThresholds value) {
    return new FixedPolicies(
        freshness, value, briefings, coldRule, circuit, auth, aiCallsPerMinute, expiryRules);
  }

  public FixedPolicies withExpiryRules(ExpiryRules value) {
    return new FixedPolicies(
        freshness, thresholds, briefings, coldRule, circuit, auth, aiCallsPerMinute, value);
  }

  @Override
  public FreshnessPolicy freshness(HouseholdId household) {
    return freshness;
  }

  @Override
  public ExpiryRules expiryRules(HouseholdId household) {
    return expiryRules;
  }

  @Override
  public FridgeThresholds thresholds(HouseholdId household) {
    return thresholds;
  }

  @Override
  public FridgeThresholds thresholdsOf(FridgeId fridge) {
    return thresholds;
  }

  @Override
  public BriefingSchedule briefings(HouseholdId household) {
    return briefings;
  }
}
