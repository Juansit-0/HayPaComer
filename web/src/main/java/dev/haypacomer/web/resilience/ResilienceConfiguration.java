package dev.haypacomer.web.resilience;

import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.RecipeTemplateRepository;
import dev.haypacomer.application.port.ServiceHealth;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.application.resilience.ViewServiceStatus;
import dev.haypacomer.persistence.relational.PostgresFoodCatalogRepository;
import dev.haypacomer.persistence.relational.PostgresFoodPriceRepository;
import dev.haypacomer.persistence.relational.PostgresRecipeRepository;
import dev.haypacomer.persistence.relational.PostgresSubstitutionRuleRepository;
import dev.haypacomer.persistence.resilience.InMemoryServiceHealth;
import dev.haypacomer.persistence.resilience.ResilientFoodCatalog;
import dev.haypacomer.persistence.resilience.ResilientFoodPrices;
import dev.haypacomer.persistence.resilience.ResilientReads;
import dev.haypacomer.persistence.resilience.ResilientRecipeTemplates;
import dev.haypacomer.persistence.resilience.ResilientSubstitutionRules;
import dev.haypacomer.persistence.resilience.RetryPolicy;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ResilienceConfiguration {

  @Bean
  ServiceHealth serviceHealth() {
    return new InMemoryServiceHealth();
  }

  @Bean
  RetryPolicy referenceDataRetry() {
    return new RetryPolicy(3, Duration.ofMillis(100), 2, RetryPolicy.THREAD_SLEEP);
  }

  @Bean
  ViewServiceStatus viewServiceStatus(ServiceHealth health) {
    return new ViewServiceStatus(health);
  }

  @Bean
  @Primary
  FoodCatalogRepository resilientFoodCatalog(
      PostgresFoodCatalogRepository catalog, RetryPolicy retry, ServiceHealth health, Clock clock) {
    return new ResilientFoodCatalog(
        catalog, new ResilientReads("food catalog", retry, health, clock));
  }

  @Bean
  @Primary
  SubstitutionRuleRepository resilientSubstitutionRules(
      PostgresSubstitutionRuleRepository rules,
      RetryPolicy retry,
      ServiceHealth health,
      Clock clock) {
    return new ResilientSubstitutionRules(
        rules, new ResilientReads("substitution rules", retry, health, clock));
  }

  @Bean
  @Primary
  RecipeTemplateRepository resilientRecipeTemplates(
      PostgresRecipeRepository recipes, RetryPolicy retry, ServiceHealth health, Clock clock) {
    return new ResilientRecipeTemplates(
        recipes, new ResilientReads("recipe templates", retry, health, clock));
  }

  @Bean
  @Primary
  FoodPriceRepository resilientFoodPrices(
      PostgresFoodPriceRepository prices, RetryPolicy retry, ServiceHealth health, Clock clock) {
    return new ResilientFoodPrices(prices, new ResilientReads("food prices", retry, health, clock));
  }
}
