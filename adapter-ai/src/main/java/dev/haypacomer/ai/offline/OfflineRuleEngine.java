package dev.haypacomer.ai.offline;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.StatusExplanation;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.domain.inventory.StockedFood;
import java.time.LocalDate;
import java.util.Optional;

public final class OfflineRuleEngine implements KitchenAdvisor {

  private final RecipeRanker ranker = new RecipeRanker();
  private final StatusRules status = new StatusRules();

  @Override
  public Suggestions suggest(SuggestionRequest request) {
    return new Suggestions(ranker.rank(request), AdvisorSource.OFFLINE_RULES);
  }

  @Override
  public Optional<ParsedIntent> parseIntent(String text, LocalDate today) {
    return IntentRules.parse(text, today);
  }

  @Override
  public StatusExplanation explain(StockedFood food, LocalDate today) {
    return status.explain(food, today);
  }
}
