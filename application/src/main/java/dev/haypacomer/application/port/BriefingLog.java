package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import java.time.LocalDate;

public interface BriefingLog {

  boolean claim(HouseholdId household, String kind, LocalDate day);
}
