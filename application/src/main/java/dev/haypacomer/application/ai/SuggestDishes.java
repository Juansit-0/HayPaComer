package dev.haypacomer.application.ai;

import dev.haypacomer.application.cooking.KitchenStock;
import dev.haypacomer.application.cooking.ReadKitchenStock;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.AiRateLimiter;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.application.profile.SelectDiners;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class SuggestDishes {

  public static final int CALLS_PER_MINUTE = 20;

  private final ReadKitchenStock stock;
  private final SelectDiners diners;
  private final SubstitutionRuleRepository rules;
  private final KitchenAdvisor advisor;
  private final AiRateLimiter rateLimiter;

  public SuggestDishes(
      HouseholdRepository households,
      ViewInventory inventory,
      FoodProfileRepository profiles,
      SubstitutionRuleRepository rules,
      KitchenAdvisor advisor,
      AiRateLimiter rateLimiter,
      Clock clock) {
    this.stock = new ReadKitchenStock(households, inventory, clock);
    this.diners = new SelectDiners(households, profiles);
    this.rules = Objects.requireNonNull(rules, "rules");
    this.advisor = Objects.requireNonNull(advisor, "advisor");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
  }

  public Suggestions suggest(UserId actor, HouseholdId householdId, SuggestionQuery query) {
    KitchenStock kitchen = stock.read(actor, householdId);
    if (!rateLimiter.tryAcquire(
        actor.value().toString(), CALLS_PER_MINUTE, Duration.ofMinutes(1))) {
      throw new AiRateLimitExceededException();
    }
    List<Recipe> candidates =
        query.rescueOnly()
            ? query.candidates().stream()
                .filter(
                    recipe ->
                        recipe.requirements().stream()
                            .map(RecipeRequirement::food)
                            .anyMatch(food -> kitchen.atRiskFoods().contains(food.key())))
                .toList()
            : query.candidates();
    return advisor.suggest(
        new SuggestionRequest(
            candidates,
            kitchen.availability(),
            kitchen.atRiskFoods(),
            diners.select(actor, householdId, query.diners()),
            new SubstitutionCatalog(rules.all()),
            query.servings(),
            query.maxMinutes(),
            query.limit()));
  }
}
