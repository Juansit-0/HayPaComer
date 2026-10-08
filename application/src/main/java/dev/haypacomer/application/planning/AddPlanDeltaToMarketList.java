package dev.haypacomer.application.planning;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.port.WeeklyPlanRepository;
import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.planning.WeeklyPlan;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AddPlanDeltaToMarketList {

  private final GetHousehold households;
  private final WeeklyPlanRepository plans;
  private final ViewInventory inventory;
  private final MarketListRepository lists;
  private final Clock clock;

  public AddPlanDeltaToMarketList(
      HouseholdRepository households,
      WeeklyPlanRepository plans,
      ViewInventory inventory,
      MarketListRepository lists,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.plans = Objects.requireNonNull(plans, "plans");
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.lists = Objects.requireNonNull(lists, "lists");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<PlanDeltaItem> add(UserId actor, HouseholdId householdId) {
    Household household = households.get(actor, householdId);
    household.requirePermission(actor, Permission.MANAGE_MARKET_LIST);
    Instant now = clock.instant();
    LocalDate today = LocalDate.ofInstant(now, household.timezone());
    WeeklyPlan plan =
        plans.current(householdId, today).orElseThrow(WeeklyPlanNotFoundException::new);
    Map<String, FoodMetadata> foods = new LinkedHashMap<>();
    Map<String, Grams> needed = new LinkedHashMap<>();
    plan.entries().stream()
        .filter(entry -> !plan.dateOf(entry).isBefore(today))
        .flatMap(entry -> entry.recipe().scaledTo(entry.servings()).requirements().stream())
        .filter(requirement -> !requirement.optional())
        .forEach(
            requirement -> {
              foods.putIfAbsent(requirement.food().key(), requirement.food());
              needed.merge(requirement.food().key(), requirement.grams(), Grams::plus);
            });
    Availability availability = new Availability();
    inventory.view(actor, householdId, today).stream()
        .filter(InventoryEntry::usable)
        .filter(entry -> entry.food().isEdible())
        .forEach(
            entry -> availability.add(entry.food().item().food(), entry.food().item().quantity()));
    MarketList list =
        lists.findByHousehold(householdId).orElseGet(() -> MarketList.empty(householdId));
    List<PlanDeltaItem> delta = new ArrayList<>();
    for (Map.Entry<String, Grams> total : needed.entrySet()) {
      FoodMetadata food = foods.get(total.getKey());
      Grams available = availability.of(food);
      Grams gap = available.shortfallTo(total.getValue());
      if (gap.isZero()) {
        continue;
      }
      Grams before = list.pendingGrams(food);
      list.topUp(food, gap, MarketSource.PLAN, actor, now);
      Grams after = list.pendingGrams(food);
      delta.add(new PlanDeltaItem(food, total.getValue(), available, after.minus(before), after));
    }
    if (!delta.isEmpty()) {
      lists.save(list);
    }
    return List.copyOf(delta);
  }
}
