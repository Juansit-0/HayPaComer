package dev.haypacomer.application.cooking;

import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.inventory.InventoryEntry;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.FoodProfileRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.application.profile.ListFoodProfiles;
import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.cooking.EvaluationStrategy;
import dev.haypacomer.domain.cooking.FlexibleStrategy;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RescueStrategy;
import dev.haypacomer.domain.cooking.StrictStrategy;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class EvaluateRecipe {

  private final GetHousehold households;
  private final ViewInventory inventory;
  private final ListFoodProfiles profiles;
  private final SubstitutionRuleRepository rules;
  private final Clock clock;

  public EvaluateRecipe(
      HouseholdRepository households,
      ViewInventory inventory,
      FoodProfileRepository profiles,
      SubstitutionRuleRepository rules,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.profiles = new ListFoodProfiles(households, profiles);
    this.rules = Objects.requireNonNull(rules, "rules");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public RecipeEvaluation evaluate(
      UserId actor,
      HouseholdId householdId,
      Recipe recipe,
      int servings,
      StrategyKind kind,
      Set<MemberId> diners) {
    Household household = households.get(actor, householdId);
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    Availability availability = new Availability();
    inventory.view(actor, householdId, today).stream()
        .filter(InventoryEntry::usable)
        .filter(entry -> entry.food().isEdible())
        .forEach(
            entry -> availability.add(entry.food().item().food(), entry.food().item().quantity()));
    EvaluationStrategy strategy =
        switch (kind) {
          case STRICT -> new StrictStrategy();
          case FLEXIBLE -> FlexibleStrategy.standard();
          case RESCUE ->
              new RescueStrategy(
                  new SubstitutionCatalog(rules.all()),
                  diningGroup(actor, householdId, diners),
                  FlexibleStrategy.standard());
        };
    return strategy.evaluate(recipe, availability, servings);
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
