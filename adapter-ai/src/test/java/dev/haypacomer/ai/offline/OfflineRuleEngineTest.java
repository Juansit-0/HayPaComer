package dev.haypacomer.ai.offline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.IntentAction;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.Suggestion;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.application.ai.Urgency;
import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.cooking.RequirementVerdict;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.inventory.AtRiskFood;
import dev.haypacomer.domain.inventory.ExpiredFood;
import dev.haypacomer.domain.inventory.LeftoverFood;
import dev.haypacomer.domain.inventory.PlainFood;
import dev.haypacomer.domain.inventory.StockedFood;
import dev.haypacomer.domain.inventory.UnderReviewFood;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OfflineRuleEngineTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);
  private static final FoodMetadata EGG =
      new FoodMetadata(
          "Egg",
          FoodCategory.EGGS,
          Unit.PIECE,
          ConversionFactors.withPieceWeight("50"),
          true,
          21,
          Set.of(Allergen.EGGS));
  private static final FoodMetadata TUNA = food("Tuna", FoodCategory.FISH);

  private final OfflineRuleEngine engine = new OfflineRuleEngine();

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static Recipe recipe(String name, int minutes, RecipeRequirement... requirements) {
    return new Recipe(
        RecipeId.newId(), name, 2, minutes, RecipeSource.MANUAL, List.of(requirements), List.of());
  }

  private static final Recipe RICE_WITH_CHICKEN =
      recipe(
          "Rice with chicken",
          35,
          RecipeRequirement.of(CHICKEN, Grams.of(200)),
          RecipeRequirement.of(RICE, Grams.of(150)));
  private static final Recipe FRIED_RICE =
      recipe(
          "Fried rice",
          20,
          RecipeRequirement.of(RICE, Grams.of(200)),
          RecipeRequirement.of(EGG, Grams.of(100)));
  private static final Recipe PLAIN_RICE =
      recipe("Plain rice", 15, RecipeRequirement.of(RICE, Grams.of(150)));
  private static final Recipe TUNA_SALAD =
      recipe("Tuna salad", 10, RecipeRequirement.of(TUNA, Grams.of(500)));

  private SuggestionRequest request(DiningGroup diners, Integer minutes, int limit) {
    return new SuggestionRequest(
        List.of(PLAIN_RICE, FRIED_RICE, RICE_WITH_CHICKEN, TUNA_SALAD),
        new Availability()
            .add(CHICKEN, Grams.of(80))
            .add(RICE, Grams.of(1_000))
            .add(EGG, Grams.of(150))
            .add(TUNA, Grams.of(130)),
        Set.of(CHICKEN.key()),
        diners,
        new SubstitutionCatalog(List.of(SubstitutionRule.of(CHICKEN, TUNA, "1", 300))),
        2,
        minutes,
        limit);
  }

  @Test
  void ranksRescueFirstAndExplainsEveryOption() {
    Suggestions suggestions = engine.suggest(request(DiningGroup.of(), null, 3));

    assertEquals(AdvisorSource.OFFLINE_RULES, suggestions.source());
    assertEquals(
        List.of("Rice with chicken", "Plain rice", "Fried rice"),
        suggestions.items().stream()
            .map(suggestion -> suggestion.evaluation().recipe().name())
            .toList());
    Suggestion first = suggestions.items().getFirst();
    assertEquals(List.of(CHICKEN), first.rescuedFoods());
    assertEquals(RequirementVerdict.SUBSTITUTE, first.evaluation().verdict());
    assertEquals(
        "Uses Chicken breast before it expires. Works with an allowed substitute.", first.reason());
    assertEquals("Everything is at home for 2.", suggestions.items().get(1).reason());
  }

  @Test
  void respectsDinersTimeAndLimit() {
    DiningGroup eggAllergy =
        DiningGroup.of(
            new FoodProfile(MemberId.newId(), Diet.OMNIVORE, Set.of(Allergen.EGGS), Set.of()));

    List<String> names =
        engine.suggest(request(eggAllergy, 30, 3)).items().stream()
            .map(suggestion -> suggestion.evaluation().recipe().name())
            .toList();

    assertEquals(List.of("Plain rice"), names);
    assertEquals(1, engine.suggest(request(DiningGroup.of(), null, 1)).items().size());
  }

  @Test
  void reducesWhenNoSubstituteFits() {
    SuggestionRequest noRules =
        new SuggestionRequest(
            List.of(RICE_WITH_CHICKEN),
            new Availability().add(CHICKEN, Grams.of(80)).add(RICE, Grams.of(300)),
            Set.of(),
            DiningGroup.of(),
            SubstitutionCatalog.EMPTY,
            2,
            null,
            3);

    Suggestion only = engine.suggest(noRules).items().getFirst();

    assertEquals(RequirementVerdict.REDUCE, only.evaluation().verdict());
    assertEquals("Enough for 1 of 2 servings.", only.reason());
    assertTrue(only.rescuedFoods().isEmpty());
  }

  @Test
  void parsesSpanishAndEnglishKitchenSentences() {
    ParsedIntent soup =
        engine.parseIntent("Save 300 g of soup that expires Friday", TODAY).orElseThrow();
    assertEquals(IntentAction.STOCK, soup.action());
    assertEquals("soup", soup.food());
    assertEquals("300 g", soup.amount().orElseThrow().toString());
    assertEquals(LocalDate.of(2026, 10, 9), soup.expiry().orElseThrow());
    assertEquals(1.0, soup.confidence());

    ParsedIntent rice =
        engine
            .parseIntent("Guarda 1 1/2 tazas de arroz que vence el miércoles", TODAY)
            .orElseThrow();
    assertEquals("arroz", rice.food());
    assertEquals("1.5 cup", rice.amount().orElseThrow().toString());
    assertEquals(LocalDate.of(2026, 10, 14), rice.expiry().orElseThrow());

    ParsedIntent ate = engine.parseIntent("Me comí 150 g de pollo hoy", TODAY).orElseThrow();
    assertEquals(IntentAction.CONSUME, ate.action());
    assertEquals("pollo", ate.food());
    assertTrue(ate.expiry().isEmpty());

    ParsedIntent tossed = engine.parseIntent("Boté el yogur", TODAY).orElseThrow();
    assertEquals(IntentAction.DISCARD, tossed.action());
    assertEquals("yogur", tossed.food());
    assertTrue(tossed.amount().isEmpty());
    assertEquals(0.6, tossed.confidence(), 0.0001);

    ParsedIntent market =
        engine.parseIntent("add 1 kg of rice to the market list", TODAY).orElseThrow();
    assertEquals(IntentAction.ADD_TO_MARKET, market.action());
    assertEquals("rice", market.food());

    assertEquals("leche", engine.parseIntent("necesito comprar leche", TODAY).orElseThrow().food());
    assertEquals("huevos", engine.parseIntent("guarda 3 huevos", TODAY).orElseThrow().food());
  }

  @Test
  void readsRelativeAndExactExpiryDates() {
    assertEquals(
        TODAY.plusDays(1),
        engine.parseIntent("store milk until tomorrow", TODAY).orElseThrow().expiresOn());
    assertEquals(
        TODAY.plusDays(2),
        engine.parseIntent("guarda queso hasta pasado mañana", TODAY).orElseThrow().expiresOn());
    assertEquals(
        TODAY.plusDays(4),
        engine.parseIntent("put cheese, it expires in 4 days", TODAY).orElseThrow().expiresOn());
    assertEquals(
        LocalDate.of(2026, 10, 20),
        engine.parseIntent("save ham expires 2026-10-20", TODAY).orElseThrow().expiresOn());
    assertEquals(TODAY, engine.parseIntent("save ham today", TODAY).orElseThrow().expiresOn());
    assertTrue(
        engine.parseIntent("save ham expires 2026-02-31", TODAY).orElseThrow().expiry().isEmpty());
  }

  @Test
  void ignoresTextItCannotUnderstand() {
    assertTrue(engine.parseIntent("hello there", TODAY).isEmpty());
    assertTrue(engine.parseIntent("save the", TODAY).isEmpty());
    assertTrue(engine.parseIntent(" ", TODAY).isEmpty());
    assertTrue(engine.parseIntent(null, TODAY).isEmpty());
    assertTrue(engine.parseIntent("save " + "rice ".repeat(50), TODAY).isEmpty());
  }

  private static StockedFood item(LocalDate expiry) {
    return new PlainFood(
        new FoodItem(FoodItemId.newId(), CHICKEN, Grams.of(300), Grams.ZERO, expiry));
  }

  @Test
  void explainsRiskComputedByTheRules() {
    assertEquals(
        Urgency.DISCARD,
        engine.explain(new ExpiredFood(item(TODAY.minusDays(1))), TODAY).urgency());
    assertEquals(Urgency.REVIEW, engine.explain(new UnderReviewFood(item(TODAY)), TODAY).urgency());
    assertEquals(
        "Eat Chicken breast today.", engine.explain(new AtRiskFood(item(TODAY)), TODAY).message());
    assertEquals(
        "Chicken breast expires in 1 day. Use it soon.",
        engine.explain(new AtRiskFood(item(TODAY.plusDays(1))), TODAY).message());
    assertEquals(
        "Chicken breast expires in 2 days. Use it soon.",
        engine.explain(new AtRiskFood(item(TODAY.plusDays(2))), TODAY).message());
    assertEquals(
        Urgency.CONSUME_SOON, engine.explain(new LeftoverFood(item(null)), TODAY).urgency());
    assertEquals(Urgency.OK, engine.explain(item(null), TODAY).urgency());
    assertEquals(
        "Chicken breast expired. Do not eat it.",
        engine.explain(new ExpiredFood(item(null)), TODAY).message());
  }
}
