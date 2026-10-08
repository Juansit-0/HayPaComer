package dev.haypacomer.ai.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.ai.llm.AiUnavailableException;
import dev.haypacomer.ai.llm.InvalidAiResponseException;
import dev.haypacomer.ai.offline.OfflineRuleEngine;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitPhase;
import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.ai.CircuitState;
import dev.haypacomer.application.ai.ParsedIntent;
import dev.haypacomer.application.ai.StatusExplanation;
import dev.haypacomer.application.ai.SuggestionRequest;
import dev.haypacomer.application.ai.Suggestions;
import dev.haypacomer.application.port.CircuitBreakerStore;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.domain.cooking.Availability;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.inventory.PlainFood;
import dev.haypacomer.domain.inventory.StockedFood;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.substitution.SubstitutionCatalog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ResilientKitchenAdvisorTest {

  private static final Instant T0 = Instant.parse("2026-10-08T20:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
  private static final CircuitPolicy POLICY = new CircuitPolicy(2, Duration.ofSeconds(60));

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
  private final Map<AdvisorSource, CircuitState> states = new EnumMap<>(AdvisorSource.class);
  private final CircuitBreakerStore breaker =
      new CircuitBreakerStore() {
        @Override
        public CircuitState load(AdvisorSource provider) {
          return states.getOrDefault(provider, CircuitState.CLOSED);
        }

        @Override
        public void save(AdvisorSource provider, CircuitState state) {
          states.put(provider, state);
        }
      };

  private static final class FlakyProvider implements KitchenAdvisor {

    final AtomicInteger calls = new AtomicInteger();
    volatile RuntimeException failure;

    private void call() {
      calls.incrementAndGet();
      if (failure != null) {
        throw failure;
      }
    }

    @Override
    public Suggestions suggest(SuggestionRequest request) {
      call();
      return new Suggestions(List.of(), AdvisorSource.GEMINI);
    }

    @Override
    public Optional<ParsedIntent> parseIntent(String text, LocalDate today) {
      call();
      return Optional.empty();
    }

    @Override
    public StatusExplanation explain(StockedFood food, LocalDate today) {
      call();
      return new OfflineRuleEngine().explain(food, today);
    }
  }

  private final FlakyProvider provider = new FlakyProvider();
  private final ResilientKitchenAdvisor advisor =
      new ResilientKitchenAdvisor(
          provider, AdvisorSource.GEMINI, new OfflineRuleEngine(), breaker, POLICY, clock);

  private static final SuggestionRequest REQUEST =
      new SuggestionRequest(
          List.of(),
          new Availability(),
          Set.of(),
          DiningGroup.of(),
          SubstitutionCatalog.EMPTY,
          1,
          null,
          3);

  @Test
  void usesTheProviderWhileItWorks() {
    assertEquals(AdvisorSource.GEMINI, advisor.suggest(REQUEST).source());
    assertEquals(1, provider.calls.get());
    assertEquals(CircuitState.CLOSED, breaker.load(AdvisorSource.GEMINI));
  }

  @Test
  void fallsBackToTheRulesAndOpensAfterRepeatedFailures() {
    provider.failure = new AiUnavailableException("down");

    assertEquals(AdvisorSource.OFFLINE_RULES, advisor.suggest(REQUEST).source());
    assertEquals(CircuitPhase.CLOSED, breaker.load(AdvisorSource.GEMINI).phase());
    provider.failure = new InvalidAiResponseException("bad json");
    assertEquals("soup", advisor.parseIntent("save 300 g of soup", TODAY).orElseThrow().food());
    assertEquals(CircuitPhase.OPEN, breaker.load(AdvisorSource.GEMINI).phase());

    now.set(T0.plusSeconds(30));
    advisor.suggest(REQUEST);
    assertEquals(2, provider.calls.get());

    now.set(T0.plusSeconds(61));
    advisor.suggest(REQUEST);
    assertEquals(3, provider.calls.get());
    assertEquals(CircuitPhase.OPEN, breaker.load(AdvisorSource.GEMINI).phase());
    assertEquals(T0.plusSeconds(61), breaker.load(AdvisorSource.GEMINI).openedAt());

    provider.failure = null;
    now.set(T0.plusSeconds(122));
    FoodItem rice =
        new FoodItem(
            FoodItemId.newId(),
            new FoodMetadata(
                "Rice",
                FoodCategory.GRAIN,
                Unit.GRAM,
                ConversionFactors.MASS_ONLY,
                false,
                365,
                Set.of()),
            Grams.of(500),
            Grams.ZERO,
            null);
    advisor.explain(new PlainFood(rice), TODAY);
    assertEquals(CircuitState.CLOSED, breaker.load(AdvisorSource.GEMINI));
    assertEquals(4, provider.calls.get());
  }

  @Test
  void otherErrorsAreNotHidden() {
    provider.failure = new IllegalStateException("bug");

    assertThrows(IllegalStateException.class, () -> advisor.suggest(REQUEST));
  }
}
