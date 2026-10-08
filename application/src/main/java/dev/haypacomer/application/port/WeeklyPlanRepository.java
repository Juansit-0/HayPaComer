package dev.haypacomer.application.port;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.planning.PlanEntryId;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.planning.WeeklyPlanId;
import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyPlanRepository {

  void save(WeeklyPlan plan);

  Optional<WeeklyPlan> find(HouseholdId household, WeeklyPlanId id);

  Optional<WeeklyPlan> current(HouseholdId household, LocalDate today);

  Optional<WeeklyPlan> findByEntry(HouseholdId household, PlanEntryId entry);
}
