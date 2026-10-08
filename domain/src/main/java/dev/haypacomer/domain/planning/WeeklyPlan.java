package dev.haypacomer.domain.planning;

import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.recipe.Recipe;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WeeklyPlan {

  private final WeeklyPlanId id;
  private final HouseholdId household;
  private final LocalDate weekStart;
  private final List<PlanEntry> entries;
  private final WeeklyPlanId clonedFrom;

  private WeeklyPlan(
      WeeklyPlanId id,
      HouseholdId household,
      LocalDate weekStart,
      List<PlanEntry> entries,
      WeeklyPlanId clonedFrom) {
    this.id = Objects.requireNonNull(id, "id");
    this.clonedFrom = clonedFrom;
    this.household = Objects.requireNonNull(household, "household");
    this.weekStart = Objects.requireNonNull(weekStart, "weekStart");
    this.entries = new ArrayList<>(entries);
    Set<Integer> slots = new HashSet<>();
    for (PlanEntry entry : entries) {
      if (!slots.add(entry.slot())) {
        throw new IllegalArgumentException(
            "Day " + entry.day() + " already has a " + entry.meal() + " planned");
      }
    }
    this.entries.sort(Comparator.comparingInt(PlanEntry::slot));
  }

  public static WeeklyPlan create(
      HouseholdId household, LocalDate weekStart, List<PlanEntry> entries) {
    return new WeeklyPlan(WeeklyPlanId.newId(), household, weekStart, entries, null);
  }

  public static WeeklyPlan restore(
      WeeklyPlanId id,
      HouseholdId household,
      LocalDate weekStart,
      List<PlanEntry> entries,
      WeeklyPlanId clonedFrom) {
    return new WeeklyPlan(id, household, weekStart, entries, clonedFrom);
  }

  public WeeklyPlan cloneFor(LocalDate newWeekStart) {
    Objects.requireNonNull(newWeekStart, "newWeekStart");
    if (newWeekStart.equals(weekStart)) {
      throw new IllegalArgumentException("A copy must start on a different week");
    }
    return new WeeklyPlan(
        WeeklyPlanId.newId(),
        household,
        newWeekStart,
        entries.stream()
            .map(
                entry ->
                    PlanEntry.of(
                        entry.day(),
                        entry.meal(),
                        entry.recipe(),
                        entry.servings(),
                        entry.needsShopping()))
            .toList(),
        id);
  }

  public Optional<WeeklyPlanId> clonedFrom() {
    return Optional.ofNullable(clonedFrom);
  }

  public WeeklyPlanId id() {
    return id;
  }

  public HouseholdId household() {
    return household;
  }

  public LocalDate weekStart() {
    return weekStart;
  }

  public LocalDate weekEnd() {
    return weekStart.plusDays(PlanEntry.DAYS - 1L);
  }

  public boolean covers(LocalDate date) {
    return !date.isBefore(weekStart) && !date.isAfter(weekEnd());
  }

  public List<PlanEntry> entries() {
    return List.copyOf(entries);
  }

  public LocalDate dateOf(PlanEntry entry) {
    return weekStart.plusDays(entry.day() - 1L);
  }

  public Optional<PlanEntry> entry(PlanEntryId entryId) {
    return entries.stream().filter(entry -> entry.id().equals(entryId)).findFirst();
  }

  public PlanEntry change(PlanEntryId entryId, Recipe recipe, int servings, boolean shopping) {
    PlanEntry current =
        entry(entryId)
            .orElseThrow(() -> new IllegalArgumentException("The plan has no such entry"));
    PlanEntry changed =
        new PlanEntry(current.id(), current.day(), current.meal(), recipe, servings, shopping);
    entries.set(entries.indexOf(current), changed);
    return changed;
  }
}
