package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import java.util.List;

public interface HouseholdDirectory {

  List<HouseholdId> all();
}
