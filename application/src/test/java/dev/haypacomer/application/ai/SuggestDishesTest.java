package dev.haypacomer.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.port.AiRateLimiter;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.application.profile.UpdateFoodProfile;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryCookingStores;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.StockedFood;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuggestDishesTest {

  private static final Instant NOW = Instant.parse("2026-10-08T18:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);
  private static final FoodMetadata TUNA =
      new FoodMetadata(
          "Tuna",
          FoodCategory.FISH,
          Unit.GRAM,
          ConversionFactors.MASS_ONLY,
          true,
          3,
          Set.of(Allergen.FISH));
  private static final Recipe CHICKEN_RICE =
      recipe("Rice with chicken", RecipeRequirement.of(CHICKEN, Grams.of(200)));
  private static final Recipe PLAIN_RICE =
      recipe("Plain rice", RecipeRequirement.of(RICE, Grams.of(150)));

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final InMemoryCookingStores cooking = new InMemoryCookingStores();
  private final List<SuggestionRequest> requests = new ArrayList<>();
  private final List<String> limited = new ArrayList<>();
  private boolean allow = true;
  private final KitchenAdvisor advisor =
      new KitchenAdvisor() {
        @Override
        public Suggestions suggest(SuggestionRequest request) {
          requests.add(request);
          return new Suggestions(List.of(), AdvisorSource.OFFLINE_RULES);
        }

        @Override
        public Optional<ParsedIntent> parseIntent(String text, LocalDate today) {
          return Optional.empty();
        }

        @Override
        public StatusExplanation explain(StockedFood food, LocalDate today) {
          return new StatusExplanation("x", Urgency.OK, "x", AdvisorSource.OFFLINE_RULES);
        }
      };
  private final AiRateLimiter limiter =
      (subject, limit, window) -> {
        limited.add(subject + "/" + limit + "/" + window);
        return allow;
      };
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household household;
  private SuggestDishes suggest;

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static Recipe recipe(String name, RecipeRequirement... requirements) {
    return new Recipe(
        RecipeId.newId(), name, 2, 20, RecipeSource.MANUAL, List.of(requirements), List.of());
  }

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    households.save(household);
    Fridge fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    fridge.place(
        new FoodItem(FoodItemId.newId(), CHICKEN, Grams.of(300), Grams.ZERO, TODAY.plusDays(1)),
        fridge.trays().findFirst().orElseThrow().id());
    fridge.place(
        new FoodItem(FoodItemId.newId(), RICE, Grams.of(500), Grams.ZERO, null),
        fridge.trays().findFirst().orElseThrow().id());
    cooking.rules.save(SubstitutionRule.of(CHICKEN, TUNA, "1", 300));
    suggest =
        new SuggestDishes(
            households,
            new ViewInventory(
                households,
                fridges,
                stores.ownerships,
                new InMemoryColdChainRepository(),
                FreshnessPolicy.DEFAULT),
            cooking.profiles,
            cooking.rules,
            advisor,
            limiter,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void passesTheRealStockProfilesAndRulesToTheAdvisor() {
    new UpdateFoodProfile(households, cooking.profiles)
        .update(ana, household.id(), Diet.OMNIVORE, Set.of(Allergen.FISH), Set.of());

    suggest.suggest(
        juan,
        household.id(),
        SuggestionQuery.builder()
            .candidate(CHICKEN_RICE)
            .candidate(PLAIN_RICE)
            .servings(3)
            .maxMinutes(30)
            .limit(2)
            .build());

    SuggestionRequest request = requests.getFirst();
    assertEquals(2, request.candidates().size());
    assertEquals(Grams.of(300), request.availability().of(CHICKEN));
    assertEquals(Set.of(CHICKEN.key()), request.atRiskFoods());
    assertEquals(2, request.diners().profiles().size());
    assertFalse(request.diners().allows(TUNA));
    assertEquals(1, request.substitutions().rulesFor(CHICKEN).size());
    assertEquals(3, request.servings());
    assertEquals(30, request.minutesLimit().orElseThrow());
    assertEquals(2, request.limit());
    assertEquals(
        List.of(juan.value() + "/" + SuggestDishes.CALLS_PER_MINUTE + "/" + Duration.ofMinutes(1)),
        limited);
  }

  @Test
  void rescueModeOnlyOffersRecipesThatUseFoodAboutToExpire() {
    MemberId juanMember = household.membershipOf(juan).orElseThrow().member();

    suggest.suggest(
        ana,
        household.id(),
        SuggestionQuery.builder()
            .candidates(List.of(CHICKEN_RICE, PLAIN_RICE))
            .diner(juanMember)
            .rescueOnly(true)
            .build());

    assertEquals(List.of(CHICKEN_RICE), requests.getFirst().candidates());
    assertEquals(1, requests.getFirst().diners().profiles().size());
  }

  @Test
  void rateLimitsAndChecksMembership() {
    SuggestionQuery query = SuggestionQuery.builder().candidate(PLAIN_RICE).build();
    allow = false;

    assertThrows(
        AiRateLimitExceededException.class, () -> suggest.suggest(juan, household.id(), query));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> suggest.suggest(UserId.newId(), household.id(), query));
    assertTrue(requests.isEmpty());
  }

  @Test
  void theBuilderValidatesEveryConstraint() {
    SuggestionQuery defaults = SuggestionQuery.builder().candidate(PLAIN_RICE).build();
    assertEquals(2, defaults.servings());
    assertEquals(3, defaults.limit());
    assertTrue(defaults.minutesLimit().isEmpty());
    assertTrue(defaults.diners().isEmpty());
    assertFalse(defaults.rescueOnly());

    assertThrows(IllegalArgumentException.class, () -> SuggestionQuery.builder().build());
    assertThrows(
        IllegalArgumentException.class,
        () -> SuggestionQuery.builder().candidate(PLAIN_RICE).servings(0).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> SuggestionQuery.builder().candidate(PLAIN_RICE).maxMinutes(0).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> SuggestionQuery.builder().candidate(PLAIN_RICE).limit(4).build());
    SuggestionQuery.Builder tooMany = SuggestionQuery.builder();
    for (int index = 0; index < 51; index++) {
      tooMany.candidate(PLAIN_RICE);
    }
    assertThrows(IllegalArgumentException.class, tooMany::build);
  }
}
