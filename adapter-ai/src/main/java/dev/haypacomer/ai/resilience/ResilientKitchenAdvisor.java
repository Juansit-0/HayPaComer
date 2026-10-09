package dev.haypacomer.ai.resilience;

import dev.haypacomer.ai.llm.AiUnavailableException;
import dev.haypacomer.ai.llm.InvalidAiResponseException;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.StatusExplanation;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.domain.inventory.StockedFood;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class ResilientKitchenAdvisor implements KitchenAdvisor {

  private final KitchenAdvisor provider;
  private final KitchenAdvisor fallback;
  private final ProviderCircuit circuit;

  public ResilientKitchenAdvisor(
      KitchenAdvisor provider, KitchenAdvisor fallback, ProviderCircuit circuit) {
    this.provider = Objects.requireNonNull(provider, "provider");
    this.fallback = Objects.requireNonNull(fallback, "fallback");
    this.circuit = Objects.requireNonNull(circuit, "circuit");
  }

  @Override
  public Suggestions suggest(SuggestionRequest request) {
    return call(advisor -> advisor.suggest(request));
  }

  @Override
  public Optional<ParsedIntent> parseIntent(String text, LocalDate today) {
    return call(advisor -> advisor.parseIntent(text, today));
  }

  @Override
  public StatusExplanation explain(StockedFood food, LocalDate today) {
    return call(advisor -> advisor.explain(food, today));
  }

  private <T> T call(Function<KitchenAdvisor, T> work) {
    return circuit.call(
        () -> work.apply(provider),
        () -> work.apply(fallback),
        failure ->
            failure instanceof AiUnavailableException
                || failure instanceof InvalidAiResponseException);
  }
}
