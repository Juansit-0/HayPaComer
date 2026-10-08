package dev.haypacomer.application.cooking;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.cooking.EvaluationStrategy;
import dev.haypacomer.domain.cooking.FlexibleStrategy;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RescueStrategy;
import dev.haypacomer.domain.cooking.StrictStrategy;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.recipe.Recipe;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class EvaluateRecipe {

  private final GetHousehold households;
  private final ViewInventory inventory;
  private final Clock clock;

  public EvaluateRecipe(HouseholdRepository households, ViewInventory inventory, Clock clock) {
    this.households = new GetHousehold(households);
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public RecipeEvaluation evaluate(
      UserId actor,
      HouseholdId householdId,
      Recipe recipe,
      int servings,
      StrategyKind kind,
      Function<FoodMetadata, List<FoodMetadata>> substitutes) {
    Household household = households.get(actor, householdId);
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    Availability availability = new Availability();
    inventory.view(actor, householdId, today).stream()
        .filter(InventoryEntry::usable)
        .filter(entry -> entry.food().isEdible())
        .forEach(
            entry -> availability.add(entry.food().item().food(), entry.food().item().quantity()));
    return strategy(kind, substitutes).evaluate(recipe, availability, servings);
  }

  private static EvaluationStrategy strategy(
      StrategyKind kind, Function<FoodMetadata, List<FoodMetadata>> substitutes) {
    return switch (kind) {
      case STRICT -> new StrictStrategy();
      case FLEXIBLE -> FlexibleStrategy.standard();
      case RESCUE -> new RescueStrategy(substitutes, FlexibleStrategy.standard());
    };
  }
}
