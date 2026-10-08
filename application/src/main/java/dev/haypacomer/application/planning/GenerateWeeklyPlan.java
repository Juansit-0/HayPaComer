package dev.haypacomer.application.planning;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.RecipeRepository;
import dev.haypacomer.application.port.WeeklyPlanRepository;
import dev.haypacomer.application.profile.ListFoodProfiles;
import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FoodStatus;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.planning.PlanningContext;
import dev.haypacomer.domain.planning.PlanningStrategy;
import dev.haypacomer.domain.planning.WeeklyPlan;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class GenerateWeeklyPlan {

  private final GetHousehold households;
  private final ViewInventory inventory;
  private final ListFoodProfiles profiles;
  private final RecipeRepository recipes;
  private final WeeklyPlanRepository plans;
  private final PlanningStrategy strategy;
  private final Clock clock;

  public GenerateWeeklyPlan(
      HouseholdRepository households,
      ViewInventory inventory,
      FoodProfileRepository profiles,
      RecipeRepository recipes,
      WeeklyPlanRepository plans,
      PlanningStrategy strategy,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.profiles = new ListFoodProfiles(households, profiles);
    this.recipes = Objects.requireNonNull(recipes, "recipes");
    this.plans = Objects.requireNonNull(plans, "plans");
    this.strategy = Objects.requireNonNull(strategy, "strategy");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public WeeklyPlan generate(
      UserId actor, HouseholdId householdId, int servings, Set<MemberId> diners) {
    Household household = households.get(actor, householdId);
    household.requirePermission(actor, Permission.COOK);
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    Availability availability = new Availability();
    Set<String> atRisk = new HashSet<>();
    inventory.view(actor, householdId, today).stream()
        .filter(InventoryEntry::usable)
        .filter(entry -> entry.food().isEdible())
        .forEach(
            entry -> {
              availability.add(entry.food().item().food(), entry.food().item().quantity());
              if (entry.food().has(FoodStatus.AT_RISK) || entry.food().has(FoodStatus.LEFTOVER)) {
                atRisk.add(entry.food().item().food().key());
              }
            });
    WeeklyPlan plan =
        WeeklyPlan.create(
            householdId,
            today,
            strategy.plan(
                new PlanningContext(
                    recipes.findByHousehold(householdId),
                    availability,
                    atRisk,
                    diningGroup(actor, householdId, diners),
                    servings)));
    plans.save(plan);
    return plan;
  }

  private DiningGroup diningGroup(UserId actor, HouseholdId householdId, Set<MemberId> diners) {
    List<FoodProfile> all = profiles.list(actor, householdId);
    if (diners.isEmpty()) {
      return new DiningGroup(all);
    }
    List<FoodProfile> chosen =
        all.stream().filter(profile -> diners.contains(profile.member())).toList();
    if (chosen.size() != diners.size()) {
      throw new IllegalArgumentException("Every diner must be a member of the household");
    }
    return new DiningGroup(chosen);
  }
}
