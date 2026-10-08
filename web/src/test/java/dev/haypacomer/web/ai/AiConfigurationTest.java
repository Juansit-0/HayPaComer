package dev.haypacomer.web.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.ai.offline.OfflineRuleEngine;
import dev.haypacomer.ai.resilience.ResilientKitchenAdvisor;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitState;
import dev.haypacomer.application.port.AiResponseCache;
import dev.haypacomer.application.port.CircuitBreakerStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiConfigurationTest {

  private static final AiResponseCache NO_CACHE =
      new AiResponseCache() {
        @Override
        public Optional<String> get(String key) {
          return Optional.empty();
        }

        @Override
        public void put(String key, String json, Duration timeToLive) {}
      };
  private static final CircuitBreakerStore BREAKER =
      new CircuitBreakerStore() {
        @Override
        public CircuitState load(AdvisorSource provider) {
          return CircuitState.CLOSED;
        }

        @Override
        public void save(AdvisorSource provider, CircuitState state) {}
      };

  private static AiProperties properties(
      AiProperties.Provider provider, String geminiKey, String baseUrl, String openAiKey) {
    return new AiProperties(
        provider,
        Duration.ofSeconds(5),
        Duration.ofHours(1),
        new AiProperties.Gemini(geminiKey, "gemini-2.5-flash"),
        new AiProperties.OpenAiCompatible(baseUrl, openAiKey, "gpt-4o-mini"));
  }

  private static Object advisor(AiProperties properties) {
    return new AiConfiguration().kitchenAdvisor(properties, NO_CACHE, BREAKER, Clock.systemUTC());
  }

  @Test
  void worksOfflineByDefault() {
    assertInstanceOf(
        OfflineRuleEngine.class, advisor(properties(AiProperties.Provider.OFFLINE, "", "", "")));
  }

  @Test
  void wrapsConfiguredProvidersWithTheBreakerAndFallback() {
    assertInstanceOf(
        ResilientKitchenAdvisor.class,
        advisor(properties(AiProperties.Provider.GEMINI, "key", "", "")));
    assertInstanceOf(
        ResilientKitchenAdvisor.class,
        advisor(
            properties(
                AiProperties.Provider.OPENAI_COMPATIBLE, "", "http://localhost:11434/v1", "key")));
    assertEquals(
        AdvisorSource.OPENAI_COMPATIBLE,
        AiConfiguration.client(
                properties(
                    AiProperties.Provider.OPENAI_COMPATIBLE, "", "http://localhost/v1/", "key"))
            .source());
  }

  @Test
  void refusesToStartAProviderWithoutItsSettings() {
    assertThrows(
        IllegalStateException.class,
        () -> advisor(properties(AiProperties.Provider.GEMINI, " ", "", "")));
    assertThrows(
        IllegalStateException.class,
        () -> advisor(properties(AiProperties.Provider.OPENAI_COMPATIBLE, "", "", "key")));
    assertThrows(
        IllegalArgumentException.class,
        () -> AiConfiguration.client(properties(AiProperties.Provider.OFFLINE, "", "", "")));
  }
}
