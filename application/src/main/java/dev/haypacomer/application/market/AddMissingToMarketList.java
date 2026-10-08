package dev.haypacomer.application.market;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.cooking.StrategyKind;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RequirementEvaluation;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.market.MarketSource;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.recipe.Recipe;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AddMissingToMarketList {

  private final GetHousehold households;
  private final EvaluateRecipe evaluateRecipe;
  private final MarketListRepository lists;
  private final Clock clock;

  public AddMissingToMarketList(
      HouseholdRepository households,
      EvaluateRecipe evaluateRecipe,
      MarketListRepository lists,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.evaluateRecipe = Objects.requireNonNull(evaluateRecipe, "evaluateRecipe");
    this.lists = Objects.requireNonNull(lists, "lists");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public List<MissingItem> add(UserId actor, HouseholdId householdId, Recipe recipe, int servings) {
    households.get(actor, householdId).requirePermission(actor, Permission.MANAGE_MARKET_LIST);
    RecipeEvaluation evaluation =
        evaluateRecipe.evaluate(
            actor, householdId, recipe, servings, StrategyKind.STRICT, Set.of());
    MarketList list =
        lists.findByHousehold(householdId).orElseGet(() -> MarketList.empty(householdId));
    Instant now = clock.instant();
    List<MissingItem> missing = new ArrayList<>();
    for (RequirementEvaluation requirement : evaluation.missing()) {
      Grams before = list.pendingGrams(requirement.requirement().food());
      list.topUp(
          requirement.requirement().food(),
          requirement.shortfall(),
          MarketSource.RECIPE,
          actor,
          now);
      Grams after = list.pendingGrams(requirement.requirement().food());
      missing.add(
          new MissingItem(
              requirement.requirement().food(),
              requirement.shortfall(),
              after.minus(before),
              after));
    }
    if (!missing.isEmpty()) {
      lists.save(list);
    }
    return List.copyOf(missing);
  }
}
