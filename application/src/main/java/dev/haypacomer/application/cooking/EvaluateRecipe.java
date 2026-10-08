package dev.haypacomer.application.cooking;

import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.application.profile.SelectDiners;
import dev.haypacomer.domain.cooking.EvaluationStrategy;
import dev.haypacomer.domain.cooking.FlexibleStrategy;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RescueStrategy;
import dev.haypacomer.domain.cooking.StrictStrategy;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;

public final class EvaluateRecipe {

  private final ReadKitchenStock stock;
  private final SelectDiners diners;
  private final SubstitutionRuleRepository rules;

  public EvaluateRecipe(
      HouseholdRepository households,
      ViewInventory inventory,
      FoodProfileRepository profiles,
      SubstitutionRuleRepository rules,
      Clock clock) {
    this.stock = new ReadKitchenStock(households, inventory, clock);
    this.diners = new SelectDiners(households, profiles);
    this.rules = Objects.requireNonNull(rules, "rules");
  }

  public RecipeEvaluation evaluate(
      UserId actor,
      HouseholdId householdId,
      Recipe recipe,
      int servings,
      StrategyKind kind,
      Set<MemberId> chosenDiners) {
    KitchenStock kitchen = stock.read(actor, householdId);
    EvaluationStrategy strategy =
        switch (kind) {
          case STRICT -> new StrictStrategy();
          case FLEXIBLE -> FlexibleStrategy.standard();
          case RESCUE ->
              new RescueStrategy(
                  new SubstitutionCatalog(rules.all()),
                  diners.select(actor, householdId, chosenDiners),
                  FlexibleStrategy.standard());
        };
    return strategy.evaluate(recipe, kitchen.availability(), servings);
  }
}
