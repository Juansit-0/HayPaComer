package dev.haypacomer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.cooking.EvaluateRecipe;
import dev.haypacomer.application.cooking.StrategyKind;
import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.ViewInventory;
import dev.haypacomer.application.market.AddMissingToMarketList;
import dev.haypacomer.application.port.MarketListRepository;
import dev.haypacomer.application.profile.UpdateFoodProfile;
import dev.haypacomer.application.quantity.InterpretQuantity;
import dev.haypacomer.application.scale.ReadWeighingProgress;
import dev.haypacomer.application.session.AdvanceCookingSession;
import dev.haypacomer.application.session.CheckCookingTimers;
import dev.haypacomer.application.session.GuidedCookingMediator;
import dev.haypacomer.application.session.SessionAction;
import dev.haypacomer.application.session.StartCookingSession;
import dev.haypacomer.application.session.ViewStepTimer;
import dev.haypacomer.application.session.WeighStep;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.application.support.InMemoryCookingSessionRepository;
import dev.haypacomer.application.support.InMemoryCookingStores;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.application.support.InMemoryKitchenDevices;
import dev.haypacomer.domain.cooking.RecipeEvaluation;
import dev.haypacomer.domain.cooking.RequirementVerdict;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.market.MarketList;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.recipe.RecipeStep;
import dev.haypacomer.domain.recipe.StepWeighing;
import dev.haypacomer.domain.scale.WeighingStatus;
import dev.haypacomer.domain.session.CookingSession;
import dev.haypacomer.domain.session.SessionPhase;
import dev.haypacomer.domain.session.StepCompletion;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CookingScenarioTest {

  private static final Instant T0 = Instant.parse("2026-10-08T19:00:00Z");
  private static final FoodMetadata CHICKEN =
      food("Chicken breast", FoodCategory.POULTRY, ConversionFactors.MASS_ONLY);
  private static final FoodMetadata TUNA =
      new FoodMetadata(
          "Tuna",
          FoodCategory.FISH,
          Unit.GRAM,
          ConversionFactors.withPieceWeight("130"),
          true,
          3,
          Set.of(Allergen.FISH));
  private static final FoodMetadata RICE =
      food("Rice", FoodCategory.GRAIN, ConversionFactors.withDensity("0.85"));
  private static final FoodMetadata ONION =
      food("Onion", FoodCategory.VEGETABLE, ConversionFactors.withPieceWeight("150"));

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final InMemoryCookingStores cooking = new InMemoryCookingStores();
  private final InMemoryCookingSessionRepository sessions = new InMemoryCookingSessionRepository();
  private final InMemoryKitchenDevices kitchen = new InMemoryKitchenDevices();
  private final Map<HouseholdId, MarketList> markets = new HashMap<>();
  private final MarketListRepository marketLists =
      new MarketListRepository() {
        @Override
        public void save(MarketList list) {
          markets.put(list.household(), MarketList.restore(list.household(), list.items()));
        }

        @Override
        public Optional<MarketList> findByHousehold(HouseholdId household) {
          return Optional.ofNullable(markets.get(household))
              .map(list -> MarketList.restore(household, list.items()));
        }
      };
  private final AtomicReference<Instant> now = new AtomicReference<>(T0);
  private final Clock clock =
      new Clock() {
        @Override
        public ZoneId getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return now.get();
        }
      };
  private final GuidedCookingMediator mediator =
      new GuidedCookingMediator(kitchen.scales, kitchen.timers, kitchen.devices, kitchen.hardware);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household household;
  private Fridge fridge;
  private Device scale;
  private EvaluateRecipe evaluate;
  private Recipe riceWithChicken;

  private static FoodMetadata food(String name, FoodCategory category, ConversionFactors factors) {
    return new FoodMetadata(name, category, Unit.GRAM, factors, true, 30, Set.of());
  }

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, T0);
    household.join(ana, Role.MEMBER, T0);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    List.of(CHICKEN, TUNA, RICE, ONION).forEach(stores.catalog::save);
    cooking.rules.save(SubstitutionRule.of(CHICKEN, TUNA, "1", 150));
    scale = Device.register(household.id(), fridge.id(), "Scale", DeviceKind.ESP32_SCALE, "ab", T0);
    kitchen.devices.save(scale);
    evaluate =
        new EvaluateRecipe(
            households,
            new ViewInventory(
                households,
                fridges,
                stores.ownerships,
                new InMemoryColdChainRepository(),
                FreshnessPolicy.DEFAULT),
            cooking.profiles,
            cooking.rules,
            clock);
    InterpretQuantity interpret = new InterpretQuantity(stores.catalog);
    riceWithChicken =
        new Recipe(
            RecipeId.newId(),
            "Rice with chicken",
            2,
            35,
            RecipeSource.MANUAL,
            List.of(
                RecipeRequirement.of(
                    CHICKEN, interpret.interpret("chicken breast", "200 g").grams()),
                RecipeRequirement.of(RICE, interpret.interpret("rice", "3/4 taza").grams()),
                RecipeRequirement.optional(ONION, interpret.interpret("onion", "1/2").grams())),
            List.of(
                RecipeStep.of(1, "Weigh the chicken")
                    .withWeighing(new StepWeighing(CHICKEN, Grams.of(200))),
                RecipeStep.of(2, "Cook the rice").withTimer(Duration.ofMinutes(18)),
                RecipeStep.of(3, "Serve")));
  }

  private void stock(FoodMetadata food, long grams) {
    fridge.place(
        new FoodItem(FoodItemId.newId(), food, Grams.of(grams), Grams.ZERO, null),
        fridge.trays().findFirst().orElseThrow().id());
  }

  private RecipeEvaluation evaluate(StrategyKind kind, int servings, Set<MemberId> diners) {
    return evaluate.evaluate(juan, household.id(), riceWithChicken, servings, kind, diners);
  }

  @Test
  void portionsFollowTheQuantitiesThatAreReallyAtHome() {
    stock(CHICKEN, 80);
    stock(RICE, 400);

    assertEquals(Grams.of("153"), riceWithChicken.requirementFor(RICE).orElseThrow().grams());
    assertEquals(Grams.of(75), riceWithChicken.requirementFor(ONION).orElseThrow().grams());
    RecipeEvaluation strict = evaluate(StrategyKind.STRICT, 2, Set.of());
    RecipeEvaluation flexible = evaluate(StrategyKind.FLEXIBLE, 2, Set.of());
    RecipeEvaluation forFour = evaluate(StrategyKind.FLEXIBLE, 4, Set.of());

    assertEquals(RequirementVerdict.MISSING, strict.verdict());
    assertEquals(Grams.of(120), strict.missing().getFirst().shortfall());
    assertEquals(RequirementVerdict.REDUCE, flexible.verdict());
    assertEquals(1, flexible.achievableServings());
    assertEquals(1, forFour.achievableServings());
    assertEquals(RequirementVerdict.ENOUGH, strict.requirements().get(2).verdict());
  }

  @Test
  void substitutionsRespectProportionLimitAndTheDinersAllergies() {
    stock(CHICKEN, 80);
    stock(RICE, 400);
    stock(TUNA, 130);
    MemberId juanMember = household.membershipOf(juan).orElseThrow().member();

    RecipeEvaluation rescue = evaluate(StrategyKind.RESCUE, 2, Set.of());
    assertEquals(RequirementVerdict.SUBSTITUTE, rescue.verdict());
    assertEquals(
        Grams.of(120), rescue.requirements().getFirst().proposal().orElseThrow().substituteGrams());

    assertEquals(RequirementVerdict.REDUCE, evaluate(StrategyKind.RESCUE, 4, Set.of()).verdict());

    new UpdateFoodProfile(households, cooking.profiles)
        .update(ana, household.id(), Diet.OMNIVORE, Set.of(Allergen.FISH), Set.of());
    assertEquals(RequirementVerdict.REDUCE, evaluate(StrategyKind.RESCUE, 2, Set.of()).verdict());
    assertEquals(
        RequirementVerdict.SUBSTITUTE,
        evaluate(StrategyKind.RESCUE, 2, Set.of(juanMember)).verdict());
  }

  @Test
  void aGuidedSessionWeighsTimesAndFinishes() {
    CookingSession session =
        new StartCookingSession(households, sessions, kitchen.devices, mediator, clock)
            .start(juan, household.id(), riceWithChicken, 1, Optional.of(scale.id()));
    AdvanceCookingSession advance =
        new AdvanceCookingSession(households, sessions, mediator, clock);
    WeighStep weigh =
        new WeighStep(
            households,
            sessions,
            new ReadWeighingProgress(
                households,
                kitchen.devices,
                kitchen.samples,
                kitchen.calibrations,
                kitchen.scales,
                clock),
            mediator,
            clock);
    ViewStepTimer timer = new ViewStepTimer(households, sessions, kitchen.timers, clock);
    CheckCookingTimers ticks = new CheckCookingTimers(mediator, clock);

    advance.apply(ana, household.id(), session.id(), SessionAction.NEXT);
    assertEquals(Grams.of(100), kitchen.cookingTargets.get(scale.id()).target());
    assertEquals(
        WeighingStatus.ON_TARGET,
        weigh
            .weigh(ana, household.id(), session.id(), 1, Optional.of(Grams.of(99)))
            .progress()
            .status());
    advance.apply(ana, household.id(), session.id(), SessionAction.NEXT);
    now.set(T0.plusSeconds(600));
    advance.apply(ana, household.id(), session.id(), SessionAction.PAUSE);
    now.set(T0.plusSeconds(1_200));
    advance.apply(ana, household.id(), session.id(), SessionAction.RESUME);
    assertEquals(
        Duration.ofMinutes(8),
        timer.view(juan, household.id(), session.id()).orElseThrow().remaining());
    now.set(T0.plusSeconds(1_680));
    ticks.check();
    advance.apply(ana, household.id(), session.id(), SessionAction.NEXT);
    CookingSession done = advance.apply(ana, household.id(), session.id(), SessionAction.NEXT);

    assertEquals(SessionPhase.FINISHED, done.phase());
    assertEquals(
        List.of(1, 2, 3), done.completions().stream().map(StepCompletion::position).toList());
    assertEquals(Grams.of(99), done.completions().getFirst().measuredGrams().orElseThrow());
    assertEquals(List.of("Scale:WEIGHT_CONFIRMED_BLINK", "Scale:TIMER_DONE_BEEP"), kitchen.signals);
    assertTrue(kitchen.cookingTargets.isEmpty());
    assertTrue(kitchen.timerMap.isEmpty());
  }

  @Test
  void whatIsMissingGoesToTheMarketListOnce() {
    stock(CHICKEN, 80);
    AddMissingToMarketList addMissing =
        new AddMissingToMarketList(households, evaluate, marketLists, clock);

    addMissing.add(ana, household.id(), riceWithChicken, 2);
    addMissing.add(juan, household.id(), riceWithChicken, 2);

    MarketList list = markets.get(household.id());
    assertEquals(Grams.of(120), list.pendingGrams(CHICKEN));
    assertEquals(Grams.of("153"), list.pendingGrams(RICE));
    assertEquals(Grams.ZERO, list.pendingGrams(ONION));
  }
}
