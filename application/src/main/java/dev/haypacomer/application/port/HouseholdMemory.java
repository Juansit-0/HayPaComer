package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import java.util.Map;

public interface HouseholdMemory {

  Map<String, String> read(HouseholdId household);

  void remember(HouseholdId household, String key, String value);

  void forget(HouseholdId household, String key);
}
