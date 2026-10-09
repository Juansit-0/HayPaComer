package dev.haypacomer.web.ai;

import dev.haypacomer.agent.planning.LlmPlannerFactory;
import dev.haypacomer.agent.supervisor.OfflinePlanners;
import dev.haypacomer.agent.supervisor.PlannerFactory;
import dev.haypacomer.agent.tools.ToolRegistry;
import dev.haypacomer.ai.llm.GeminiClient;
import dev.haypacomer.ai.llm.LlmChatModel;
import dev.haypacomer.ai.llm.LlmClient;
import dev.haypacomer.ai.llm.LlmKitchenAdvisor;
import dev.haypacomer.ai.llm.LlmLabelPhotoReader;
import dev.haypacomer.ai.llm.LlmRecipePhotoReader;
import dev.haypacomer.ai.llm.LlmSettings;
import dev.haypacomer.ai.llm.OpenAiCompatibleClient;
import dev.haypacomer.ai.offline.OfflineLabelPhotoReader;
import dev.haypacomer.ai.offline.OfflineRecipePhotoReader;
import dev.haypacomer.ai.offline.OfflineRuleEngine;
import dev.haypacomer.ai.resilience.NullChatModel;
import dev.haypacomer.ai.resilience.ProviderCircuit;
import dev.haypacomer.ai.resilience.ResilientChatModel;
import dev.haypacomer.ai.resilience.ResilientKitchenAdvisor;
import dev.haypacomer.ai.resilience.ResilientLabelPhotoReader;
import dev.haypacomer.ai.resilience.ResilientRecipePhotoReader;
import dev.haypacomer.application.ai.ReadRecipePhoto;
import dev.haypacomer.application.inventory.ExpiryDesk;
import dev.haypacomer.application.inventory.ReadExpiryFromLabel;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.AiRateLimiter;
import dev.haypacomer.application.port.AiResponseCache;
import dev.haypacomer.application.port.CircuitBreakerStore;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.KitchenAdvisor;
import dev.haypacomer.application.port.LabelPhotoReader;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.RecipePhotoReader;
import dev.haypacomer.application.port.ServiceHealth;
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
      AiProperties properties,
      AiResponseCache cache,
      CircuitBreakerStore breaker,
      ServiceHealth health,
      PolicySource policies,
      Clock clock) {
    OfflineRuleEngine offline = new OfflineRuleEngine();
    if (properties.provider() == AiProperties.Provider.OFFLINE) {
      return offline;
    }
    LlmClient client = client(properties);
    return new ResilientKitchenAdvisor(
        new LlmKitchenAdvisor(client, cache, properties.cacheTimeToLive()),
        offline,
        circuit(client, breaker, health, policies, clock));
  }

  @Bean
  PlannerFactory agentPlanners(
      AiProperties properties,
      ToolRegistry agentTools,
      CircuitBreakerStore breaker,
      ServiceHealth health,
      PolicySource policies,
      Clock clock) {
    if (properties.provider() == AiProperties.Provider.OFFLINE) {
      return new OfflinePlanners();
    }
    LlmClient client = client(properties);
    return new LlmPlannerFactory(
        new ResilientChatModel(
            new LlmChatModel(client),
            new NullChatModel(),
            circuit(client, breaker, health, policies, clock)),
        agentTools);
  }

  @Bean
  RecipePhotoReader recipePhotoReader(
      AiProperties properties,
      CircuitBreakerStore breaker,
      ServiceHealth health,
      PolicySource policies,
      Clock clock) {
    if (properties.provider() == AiProperties.Provider.OFFLINE) {
      return new OfflineRecipePhotoReader();
    }
    LlmClient client = client(properties);
    return new ResilientRecipePhotoReader(
        new LlmRecipePhotoReader(client), circuit(client, breaker, health, policies, clock));
  }

  private static ProviderCircuit circuit(
      LlmClient client,
      CircuitBreakerStore breaker,
      ServiceHealth health,
      PolicySource policies,
      Clock clock) {
    return new ProviderCircuit(client.source(), breaker, policies::circuit, health, clock);
  }

  @Bean
  LabelPhotoReader labelPhotoReader(
      AiProperties properties,
      CircuitBreakerStore breaker,
      ServiceHealth health,
      PolicySource policies,
      Clock clock) {
    if (properties.provider() == AiProperties.Provider.OFFLINE) {
      return new OfflineLabelPhotoReader();
    }
    LlmClient client = client(properties);
    return new ResilientLabelPhotoReader(
        new LlmLabelPhotoReader(client), circuit(client, breaker, health, policies, clock));
  }

  @Bean
  ReadExpiryFromLabel readExpiryFromLabel(
      HouseholdRepository households,
      LabelPhotoReader reader,
      FoodCatalogRepository catalog,
      ExpiryDesk expiryDesk,
      AiRateLimiter rateLimiter,
      AiAuditLog audit,
      PolicySource policies,
      Clock clock) {
    return new ReadExpiryFromLabel(
        households, reader, catalog, expiryDesk, rateLimiter, audit, policies, clock);
  }

  @Bean
  ReadRecipePhoto readRecipePhoto(
      HouseholdRepository households,
      RecipePhotoReader reader,
      FoodCatalogRepository catalog,
      AiRateLimiter rateLimiter,
      AiAuditLog audit,
      PolicySource policies,
      Clock clock) {
    return new ReadRecipePhoto(households, reader, catalog, rateLimiter, audit, policies, clock);
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
