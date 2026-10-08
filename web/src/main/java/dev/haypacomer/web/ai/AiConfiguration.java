package dev.haypacomer.web.ai;

import dev.haypacomer.ai.llm.GeminiClient;
import dev.haypacomer.ai.llm.LlmClient;
import dev.haypacomer.ai.llm.LlmKitchenAdvisor;
import dev.haypacomer.ai.llm.LlmSettings;
import dev.haypacomer.ai.llm.OpenAiCompatibleClient;
import dev.haypacomer.ai.offline.OfflineRuleEngine;
import dev.haypacomer.ai.resilience.ResilientKitchenAdvisor;
import dev.haypacomer.application.ai.CircuitPolicy;
import dev.haypacomer.application.port.AiRateLimiter;
import dev.haypacomer.application.port.AiResponseCache;
import dev.haypacomer.application.port.CircuitBreakerStore;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.persistence.redis.RedisAiRateLimiter;
import dev.haypacomer.persistence.redis.RedisAiResponseCache;
import dev.haypacomer.persistence.redis.RedisCircuitBreakerStore;
import java.net.URI;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AiConfiguration {

  @Bean
  AiResponseCache aiResponseCache(StringRedisTemplate redis) {
    return new RedisAiResponseCache(redis);
  }

  @Bean
  CircuitBreakerStore circuitBreakerStore(StringRedisTemplate redis) {
    return new RedisCircuitBreakerStore(redis);
  }

  @Bean
  AiRateLimiter aiRateLimiter(StringRedisTemplate redis) {
    return new RedisAiRateLimiter(redis);
  }

  @Bean
  KitchenAdvisor kitchenAdvisor(
      AiProperties properties, AiResponseCache cache, CircuitBreakerStore breaker, Clock clock) {
    OfflineRuleEngine offline = new OfflineRuleEngine();
    if (properties.provider() == AiProperties.Provider.OFFLINE) {
      return offline;
    }
    LlmClient client = client(properties);
    return new ResilientKitchenAdvisor(
        new LlmKitchenAdvisor(client, cache, properties.cacheTimeToLive()),
        client.source(),
        offline,
        breaker,
        CircuitPolicy.DEFAULT,
        clock);
  }

  static LlmClient client(AiProperties properties) {
    return switch (properties.provider()) {
      case GEMINI ->
          new GeminiClient(
              new LlmSettings(
                  GeminiClient.DEFAULT_BASE_URL,
                  required(properties.gemini().apiKey(), "GEMINI_API_KEY"),
                  properties.gemini().model(),
                  properties.timeout()));
      case OPENAI_COMPATIBLE ->
          new OpenAiCompatibleClient(
              new LlmSettings(
                  URI.create(
                      withSlash(
                          required(
                              properties.openaiCompatible().baseUrl(),
                              "OPENAI_COMPATIBLE_BASE_URL"))),
                  required(properties.openaiCompatible().apiKey(), "OPENAI_COMPATIBLE_API_KEY"),
                  properties.openaiCompatible().model(),
                  properties.timeout()));
      case OFFLINE -> throw new IllegalArgumentException("The offline engine has no client");
    };
  }

  private static String required(String value, String variable) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Set " + variable + " to use this AI provider");
    }
    return value;
  }

  private static String withSlash(String url) {
    return url.endsWith("/") ? url : url + "/";
  }
}
