package dev.haypacomer.ai.llm;

import dev.haypacomer.ai.offline.RecipeRanker;
import dev.haypacomer.ai.offline.StatusRules;
import dev.haypacomer.application.ai.IntentAction;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.StatusExplanation;
import dev.haypacomer.application.ai.Suggestion;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.inventory.StockedFood;
import dev.haypacomer.domain.quantity.InvalidQuantityException;
import dev.haypacomer.domain.quantity.QuantityExpression;
import dev.haypacomer.domain.quantity.QuantityParser;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

public final class LlmKitchenAdvisor implements KitchenAdvisor {

  private static final int MAX_REASON = 300;
  private static final int MAX_FOOD = 60;
  private static final int MAX_MESSAGE = 200;
  private static final String RULES =
      "You help a home kitchen. Answer only with one JSON object that follows the requested"
          + " shape. Never invent foods, grams, dates, or safety advice that are not in the input.";

  private final LlmClient client;
  private final RecipeRanker ranker = new RecipeRanker();
  private final StatusRules status = new StatusRules();

  public LlmKitchenAdvisor(LlmClient client) {
    this.client = Objects.requireNonNull(client, "client");
  }

  @Override
  public Suggestions suggest(SuggestionRequest request) {
    List<Suggestion> cookable = ranker.rankAll(request);
    if (cookable.isEmpty()) {
      return new Suggestions(List.of(), client.source());
    }
    Map<String, Suggestion> byName = new LinkedHashMap<>();
    cookable.forEach(
        suggestion ->
            byName.put(
                suggestion.evaluation().recipe().name().toLowerCase(Locale.ROOT), suggestion));
    String options =
        cookable.stream()
            .map(
                suggestion ->
                    "- "
                        + suggestion.evaluation().recipe().name()
                        + " | verdict "
                        + suggestion.evaluation().verdict()
                        + " | "
                        + suggestion.evaluation().recipe().minutes()
                        + " min | uses expiring: "
                        + suggestion.rescuedFoods().stream()
                            .map(FoodMetadata::name)
                            .collect(Collectors.joining(", ")))
            .collect(Collectors.joining("\n"));
    JsonNode answer =
        JsonContract.object(
            client.completeJson(
                new LlmPrompt(
                    RULES,
                    "Pick at most "
                        + request.limit()
                        + " recipes from this list, best first, preferring the ones that use"
                        + " expiring food. Answer {\"choices\":[{\"recipe\":\"exact name\","
                        + "\"reason\":\"one short sentence\"}]}.\n"
                        + options)));
    JsonNode choices = answer.path("choices");
    if (!choices.isArray() || choices.size() > request.limit()) {
      throw new InvalidAiResponseException("choices must be a list of at most " + request.limit());
    }
    List<Suggestion> picked = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (JsonNode choice : choices) {
      String name = JsonContract.text(choice, "recipe", 120).toLowerCase(Locale.ROOT);
      Suggestion known = byName.get(name);
      if (known == null) {
        throw new InvalidAiResponseException("The answer names an unknown recipe");
      }
      if (!seen.add(name)) {
        throw new InvalidAiResponseException("The answer repeats a recipe");
      }
      picked.add(
          new Suggestion(
              known.evaluation(),
              known.rescuedFoods(),
              known.score(),
              JsonContract.text(choice, "reason", MAX_REASON)));
    }
    return new Suggestions(picked, client.source());
  }

  @Override
  public Optional<ParsedIntent> parseIntent(String text, LocalDate today) {
    if (text == null || text.isBlank() || text.length() > 200) {
      return Optional.empty();
    }
    JsonNode answer =
        JsonContract.object(
            client.completeJson(
                new LlmPrompt(
                    RULES,
                    "Today is "
                        + today
                        + ". Turn the sentence into {\"action\":\"STOCK|CONSUME|DISCARD|"
                        + "ADD_TO_MARKET\" or null,\"food\":\"name\",\"quantity\":\"300 g\" or"
                        + " null,\"expiresOn\":\"YYYY-MM-DD\" or null,\"confidence\":0.0-1.0}."
                        + " Sentence: "
                        + text)));
    Optional<String> action = JsonContract.optionalText(answer, "action", 20);
    if (action.isEmpty()) {
      return Optional.empty();
    }
    IntentAction intent;
    try {
      intent = IntentAction.valueOf(action.get().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException unknown) {
      throw new InvalidAiResponseException("Unknown action");
    }
    String food = JsonContract.text(answer, "food", MAX_FOOD);
    QuantityExpression quantity =
        JsonContract.optionalText(answer, "quantity", 40)
            .map(LlmKitchenAdvisor::quantity)
            .orElse(null);
    LocalDate expiresOn =
        JsonContract.optionalText(answer, "expiresOn", 10)
            .map(LlmKitchenAdvisor::date)
            .orElse(null);
    if (expiresOn != null && expiresOn.isBefore(today.minusDays(1))) {
      throw new InvalidAiResponseException("The expiry date is in the past");
    }
    return Optional.of(
        new ParsedIntent(
            intent,
            food,
            quantity,
            expiresOn,
            JsonContract.number(answer, "confidence", 0, 1),
            client.source()));
  }

  @Override
  public StatusExplanation explain(StockedFood food, LocalDate today) {
    StatusExplanation rules = status.explain(food, today);
    JsonNode answer =
        JsonContract.object(
            client.completeJson(
                new LlmPrompt(
                    RULES,
                    "Rewrite this kitchen advice in one friendly sentence without changing its"
                        + " meaning. Answer {\"message\":\"...\"}. Advice: "
                        + rules.message())));
    return new StatusExplanation(
        rules.food(),
        rules.urgency(),
        JsonContract.text(answer, "message", MAX_MESSAGE),
        client.source());
  }

  private static QuantityExpression quantity(String text) {
    try {
      return QuantityParser.parse(text);
    } catch (InvalidQuantityException unreadable) {
      throw new InvalidAiResponseException("Unreadable quantity");
    }
  }

  private static LocalDate date(String text) {
    try {
      return LocalDate.parse(text);
    } catch (DateTimeParseException invalid) {
      throw new InvalidAiResponseException("Invalid date");
    }
  }
}
