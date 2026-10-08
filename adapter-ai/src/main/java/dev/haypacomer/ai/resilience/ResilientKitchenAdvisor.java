package dev.haypacomer.ai.resilience;

import dev.haypacomer.ai.llm.AiUnavailableException;
import dev.haypacomer.ai.llm.InvalidAiResponseException;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.ai.CircuitState;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.StatusExplanation;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.application.port.CircuitBreakerStore;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.domain.inventory.StockedFood;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class ResilientKitchenAdvisor implements KitchenAdvisor {

  private final KitchenAdvisor provider;
  private final AdvisorSource source;
  private final KitchenAdvisor fallback;
  private final CircuitBreakerStore breaker;
  private final CircuitPolicy policy;
  private final Clock clock;

  public ResilientKitchenAdvisor(
      KitchenAdvisor provider,
      AdvisorSource source,
      KitchenAdvisor fallback,
      CircuitBreakerStore breaker,
      CircuitPolicy policy,
      Clock clock) {
    this.provider = Objects.requireNonNull(provider, "provider");
    this.source = Objects.requireNonNull(source, "source");
    this.fallback = Objects.requireNonNull(fallback, "fallback");
    this.breaker = Objects.requireNonNull(breaker, "breaker");
    this.policy = Objects.requireNonNull(policy, "policy");
    this.clock = Objects.requireNonNull(clock, "clock");
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
    Instant now = clock.instant();
    CircuitState state = breaker.load(source);
    if (!state.allows(now, policy)) {
      return work.apply(fallback);
    }
    CircuitState trying = state.attempt(now, policy);
    if (!trying.equals(state)) {
      breaker.save(source, trying);
    }
    try {
      T answer = work.apply(provider);
      if (!trying.equals(CircuitState.CLOSED)) {
        breaker.save(source, trying.success());
      }
      return answer;
    } catch (AiUnavailableException | InvalidAiResponseException failure) {
      breaker.save(source, trying.failure(clock.instant(), policy));
      return work.apply(fallback);
    }
  }
}
