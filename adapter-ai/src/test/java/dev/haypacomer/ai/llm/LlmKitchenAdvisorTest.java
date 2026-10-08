package dev.haypacomer.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.IntentAction;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.StatusExplanation;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.application.ai.Urgency;
import dev.haypacomer.application.port.AiResponseCache;
import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.inventory.AtRiskFood;
import dev.haypacomer.domain.inventory.PlainFood;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeRequirement;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LlmKitchenAdvisorTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);
  private static final FoodMetadata RICE = food("Rice", FoodCategory.GRAIN);

  private static FoodMetadata food(String name, FoodCategory category) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static Recipe recipe(String name, RecipeRequirement... requirements) {
    return new Recipe(
        RecipeId.newId(), name, 2, 20, RecipeSource.MANUAL, List.of(requirements), List.of());
  }

  private static final SuggestionRequest REQUEST =
      new SuggestionRequest(
          List.of(
              recipe("Plain rice", RecipeRequirement.of(RICE, Grams.of(100))),
              recipe(
                  "Rice with chicken",
                  RecipeRequirement.of(CHICKEN, Grams.of(200)),
                  RecipeRequirement.of(RICE, Grams.of(100))),
              recipe("Chicken soup", RecipeRequirement.of(CHICKEN, Grams.of(900)))),
          new Availability().add(CHICKEN, Grams.of(250)).add(RICE, Grams.of(500)),
          Set.of(CHICKEN.key()),
          DiningGroup.of(),
          SubstitutionCatalog.EMPTY,
          2,
          null,
          2);

  private static final class ScriptedClient implements LlmClient {

    private final List<String> answers;
    private final List<LlmPrompt> prompts = new ArrayList<>();

    ScriptedClient(String... answers) {
      this.answers = new ArrayList<>(List.of(answers));
    }

    @Override
    public AdvisorSource source() {
      return AdvisorSource.GEMINI;
    }

    @Override
    public String completeJson(LlmPrompt prompt) {
      prompts.add(prompt);
      return answers.removeFirst();
    }
  }

  @Test
  void keepsJavaEvaluationsAndTakesOnlyOrderAndWording() {
    ScriptedClient client =
        new ScriptedClient(
            "{\"choices\":[{\"recipe\":\"plain rice\",\"reason\":\"Quick and light.\"},"
                + "{\"recipe\":\"Rice with chicken\",\"reason\":\"Saves the chicken.\"}]}");

    Suggestions suggestions = new LlmKitchenAdvisor(client).suggest(REQUEST);

    assertEquals(AdvisorSource.GEMINI, suggestions.source());
    assertEquals("Plain rice", suggestions.items().getFirst().evaluation().recipe().name());
    assertEquals("Quick and light.", suggestions.items().getFirst().reason());
    assertEquals(List.of(CHICKEN), suggestions.items().get(1).rescuedFoods());
    String prompt = client.prompts.getFirst().user();
    assertTrue(prompt.contains("Rice with chicken | verdict ENOUGH"));
    assertFalse(prompt.contains("Chicken soup"));
  }

  @Test
  void rejectsAnswersOutsideTheContract() {
    for (String answer :
        List.of(
            "not json",
            "[1,2]",
            "{\"choices\":\"rice\"}",
            "{\"choices\":[{\"recipe\":\"Pizza\",\"reason\":\"Yum\"}]}",
            "{\"choices\":[{\"recipe\":\"Plain rice\",\"reason\":\"a\"},"
                + "{\"recipe\":\"Plain rice\",\"reason\":\"b\"}]}",
            "{\"choices\":[{\"recipe\":\"Plain rice\",\"reason\":\"a\"},{\"recipe\":\"Rice with"
                + " chicken\",\"reason\":\"b\"},{\"recipe\":\"x\",\"reason\":\"c\"}]}",
            "{\"choices\":[{\"recipe\":\"Plain rice\"}]}",
            "{\"choices\":[{\"recipe\":\"Plain rice\",\"reason\":"
                + "\""
                + "x".repeat(401)
                + "\"}]}",
            "{\"choices\":[{\"recipe\":7,\"reason\":\"a\"}]}")) {
      LlmKitchenAdvisor advisor = new LlmKitchenAdvisor(new ScriptedClient(answer));
      assertThrows(InvalidAiResponseException.class, () -> advisor.suggest(REQUEST), answer);
    }
  }

  @Test
  void doesNotCallTheProviderWhenNothingIsCookable() {
    ScriptedClient client = new ScriptedClient();
    SuggestionRequest empty =
        new SuggestionRequest(
            List.of(),
            new Availability(),
            Set.of(),
            DiningGroup.of(),
            SubstitutionCatalog.EMPTY,
            1,
            null,
            3);

    assertTrue(new LlmKitchenAdvisor(client).suggest(empty).items().isEmpty());
    assertTrue(client.prompts.isEmpty());
  }

  @Test
  void validatesParsedIntents() {
    ParsedIntent soup =
        new LlmKitchenAdvisor(
                new ScriptedClient(
                    "{\"action\":\"stock\",\"food\":\"Soup\",\"quantity\":\"300 g\","
                        + "\"expiresOn\":\"2026-10-09\",\"confidence\":0.9}"))
            .parseIntent("Save 300 g of soup that expires Friday", TODAY)
            .orElseThrow();

    assertEquals(IntentAction.STOCK, soup.action());
    assertEquals("300 g", soup.amount().orElseThrow().toString());
    assertEquals(LocalDate.of(2026, 10, 9), soup.expiry().orElseThrow());
    assertEquals(AdvisorSource.GEMINI, soup.source());
    assertTrue(
        new LlmKitchenAdvisor(new ScriptedClient("{\"action\":null}"))
            .parseIntent("hello", TODAY)
            .isEmpty());
    assertTrue(
        new LlmKitchenAdvisor(
                new ScriptedClient("{\"action\":\"DISCARD\",\"food\":\"yogurt\",\"confidence\":1}"))
            .parseIntent("tossed the yogurt", TODAY)
            .orElseThrow()
            .amount()
            .isEmpty());
    assertTrue(new LlmKitchenAdvisor(new ScriptedClient()).parseIntent(" ", TODAY).isEmpty());

    for (String answer :
        List.of(
            "{\"action\":\"EAT\",\"food\":\"rice\",\"confidence\":0.5}",
            "{\"action\":\"STOCK\",\"confidence\":0.5}",
            "{\"action\":\"STOCK\",\"food\":\"rice\",\"quantity\":\"lots\",\"confidence\":0.5}",
            "{\"action\":\"STOCK\",\"food\":\"rice\",\"expiresOn\":\"tomorrow\",\"confidence\":0.5}",
            "{\"action\":\"STOCK\",\"food\":\"rice\",\"expiresOn\":\"2020-01-01\",\"confidence\":0.5}",
            "{\"action\":\"STOCK\",\"food\":\"rice\",\"confidence\":2}",
            "{\"action\":\"STOCK\",\"food\":\"rice\",\"confidence\":\"high\"}")) {
      LlmKitchenAdvisor advisor = new LlmKitchenAdvisor(new ScriptedClient(answer));
      assertThrows(
          InvalidAiResponseException.class, () -> advisor.parseIntent("save rice", TODAY), answer);
    }
  }

  @Test
  void onlyRewordsTheRiskThatTheRulesComputed() {
    ScriptedClient client =
        new ScriptedClient("{\"message\":\"Please cook the chicken today, it will not last.\"}");
    FoodItem chicken = new FoodItem(FoodItemId.newId(), CHICKEN, Grams.of(300), Grams.ZERO, TODAY);

    StatusExplanation explanation =
        new LlmKitchenAdvisor(client).explain(new AtRiskFood(new PlainFood(chicken)), TODAY);

    assertEquals(Urgency.CONSUME_TODAY, explanation.urgency());
    assertEquals("Please cook the chicken today, it will not last.", explanation.message());
    assertTrue(client.prompts.getFirst().user().contains("Eat Chicken breast today."));
    assertThrows(
        InvalidAiResponseException.class,
        () ->
            new LlmKitchenAdvisor(new ScriptedClient("{}")).explain(new PlainFood(chicken), TODAY));
  }

  @Test
  void cachesOnlyValidatedAnswers() {
    Map<String, String> stored = new HashMap<>();
    AiResponseCache cache =
        new AiResponseCache() {
          @Override
          public Optional<String> get(String key) {
            return Optional.ofNullable(stored.get(key));
          }

          @Override
          public void put(String key, String json, Duration timeToLive) {
            assertEquals(Duration.ofHours(1), timeToLive);
            stored.put(key, json);
          }
        };
    ScriptedClient client =
        new ScriptedClient(
            "{\"action\":\"STOCK\",\"food\":\"soup\",\"confidence\":0.8}", "not json");
    LlmKitchenAdvisor advisor = new LlmKitchenAdvisor(client, cache, Duration.ofHours(1));

    advisor.parseIntent("save soup", TODAY);
    advisor.parseIntent("save soup", TODAY);
    assertEquals(1, client.prompts.size());
    assertEquals(1, stored.size());
    assertThrows(InvalidAiResponseException.class, () -> advisor.parseIntent("toss it", TODAY));
    assertEquals(1, stored.size());

    stored.replaceAll((key, json) -> "{\"action\":\"FLY\"}");
    ScriptedClient fresh =
        new ScriptedClient("{\"action\":\"STOCK\",\"food\":\"soup\",\"confidence\":0.8}");
    assertEquals(
        "soup",
        new LlmKitchenAdvisor(fresh, cache, Duration.ofHours(1))
            .parseIntent("save soup", TODAY)
            .orElseThrow()
            .food());
    assertEquals(1, fresh.prompts.size());
  }
}
