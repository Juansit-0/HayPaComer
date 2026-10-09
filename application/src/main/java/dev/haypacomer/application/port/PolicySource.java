package dev.haypacomer.application.port;

import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.auth.AuthSettings;
import dev.haypacomer.application.settings.BriefingSchedule;
import dev.haypacomer.domain.coldchain.investigation.ColdRule;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.sensor.FridgeThresholds;

public interface PolicySource {

  FreshnessPolicy freshness(HouseholdId household);

  FridgeThresholds thresholds(HouseholdId household);

  FridgeThresholds thresholdsOf(FridgeId fridge);

  BriefingSchedule briefings(HouseholdId household);

  ColdRule coldRule();

  CircuitPolicy circuit();

  AuthSettings auth();

  int aiCallsPerMinute();
}
