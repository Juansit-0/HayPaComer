package dev.haypacomer.application.port;

import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.StatusExplanation;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.domain.inventory.StockedFood;
import java.time.LocalDate;
import java.util.Optional;

public interface KitchenAdvisor {

  Suggestions suggest(SuggestionRequest request);

  Optional<ParsedIntent> parseIntent(String text, LocalDate today);

  StatusExplanation explain(StockedFood food, LocalDate today);
}
