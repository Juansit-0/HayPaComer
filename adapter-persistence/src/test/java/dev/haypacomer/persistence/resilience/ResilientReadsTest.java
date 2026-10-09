package dev.haypacomer.persistence.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.RecipeTemplateRepository;
import dev.haypacomer.application.port.SubstitutionRuleRepository;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.recipe.Recipe;
import dev.haypacomer.domain.recipe.RecipeId;
import dev.haypacomer.domain.recipe.RecipeSource;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResilientReadsTest {

  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");
  private static final FoodMetadata RICE =
      new FoodMetadata(
          "Rice",
          FoodCategory.GRAIN,
          Unit.GRAM,
          ConversionFactors.MASS_ONLY,
          false,
          365,
          java.util.Set.of());

  private final List<Duration> pauses = new ArrayList<>();
  private final RetryPolicy retry = new RetryPolicy(3, Duration.ofMillis(100), 2, pauses::add);
  private final InMemoryServiceHealth health = new InMemoryServiceHealth();
  private int failuresLeft;
  private int calls;

  private ResilientReads reads(String component) {
    return new ResilientReads(component, retry, health, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private <T> T flaky(T value) {
    calls++;
    if (failuresLeft > 0) {
      failuresLeft--;
      throw new RuntimeException("connection refused");
    }
    return value;
  }

  private final FoodCatalogRepository catalog =
      new FoodCatalogRepository() {
        @Override
        public void save(FoodMetadata food) {
          flaky(food);
        }

        @Override
        public Optional<FoodMetadata> findByName(String name) {
          return flaky(Optional.of(RICE));
        }

        @Override
        public List<FoodMetadata> search(String text, int limit) {
          return flaky(List.of(RICE));
        }
      };

  @Test
  void retriesTransientFailuresWithGrowingPauses() {
    failuresLeft = 2;

    assertEquals(
        Optional.of(RICE), new ResilientFoodCatalog(catalog, reads("catalog")).findByName("rice"));
    assertEquals(3, calls);
    assertEquals(List.of(Duration.ofMillis(100), Duration.ofMillis(200)), pauses);
    assertTrue(health.current().isEmpty());
  }

  @Test
  void servesTheLastSavedCopyAndReportsDegradedUntilRecovered() {
    ResilientFoodCatalog resilient = new ResilientFoodCatalog(catalog, reads("catalog"));
    assertEquals(List.of(RICE), resilient.search("ri", 5));
    failuresLeft = 10;

    assertEquals(List.of(RICE), resilient.search("RI", 5));
    assertEquals("catalog", health.current().getFirst().component());
    assertEquals("Serving the last saved copy", health.current().getFirst().reason());
    assertEquals(NOW, health.current().getFirst().since());
    assertThrows(RuntimeException.class, () -> resilient.findByName("rice"));
    assertEquals("Unavailable and nothing saved yet", health.current().getFirst().reason());
    assertEquals(NOW, health.current().getFirst().since());

    failuresLeft = 0;
    assertEquals(Optional.of(RICE), resilient.findByName("rice"));
    assertTrue(health.current().isEmpty());
  }

  @Test
  void writesRetryAndForgetSavedCopies() {
    ResilientFoodCatalog resilient = new ResilientFoodCatalog(catalog, reads("catalog"));
    resilient.search("ri", 5);
    failuresLeft = 1;
    resilient.save(RICE);
    failuresLeft = 10;

    assertThrows(RuntimeException.class, () -> resilient.search("ri", 5));
  }

  @Test
  void businessErrorsAreNeverRetriedOrHidden() {
    FoodCatalogRepository strict =
        new FoodCatalogRepository() {
          @Override
          public void save(FoodMetadata food) {}

          @Override
          public Optional<FoodMetadata> findByName(String name) {
            calls++;
            throw new IllegalArgumentException("bad name");
          }

          @Override
          public List<FoodMetadata> search(String text, int limit) {
            calls++;
            throw new IllegalStateException("bad state");
          }
        };
    ResilientFoodCatalog resilient = new ResilientFoodCatalog(strict, reads("catalog"));

    assertThrows(IllegalArgumentException.class, () -> resilient.findByName("x"));
    assertThrows(IllegalStateException.class, () -> resilient.search("x", 1));
    assertEquals(2, calls);
    assertTrue(health.current().isEmpty());
  }

  @Test
  void referenceDataProxiesCacheRulesTemplatesAndPrices() {
    Recipe soup =
        new Recipe(RecipeId.newId(), "Soup", 2, 30, RecipeSource.MANUAL, List.of(), List.of());
    List<SubstitutionRule> saved = new ArrayList<>();
    SubstitutionRuleRepository rules =
        new SubstitutionRuleRepository() {
          @Override
          public void save(SubstitutionRule rule) {
            saved.add(rule);
          }

          @Override
          public List<SubstitutionRule> all() {
            return flaky(List.of());
          }
        };
    RecipeTemplateRepository templates =
        new RecipeTemplateRepository() {
          @Override
          public List<Recipe> templates() {
            return flaky(List.of(soup));
          }

          @Override
          public Optional<Recipe> template(RecipeId id) {
            return flaky(Optional.of(soup));
          }
        };
    FoodPriceRepository prices =
        new FoodPriceRepository() {
          @Override
          public Map<String, BigDecimal> pricesFor(HouseholdId household, Currency currency) {
            return flaky(Map.of("rice", BigDecimal.TEN));
          }

          @Override
          public void save(HouseholdId household, String foodKey, BigDecimal pricePerKg) {
            flaky(foodKey);
          }
        };
    ResilientSubstitutionRules resilientRules =
        new ResilientSubstitutionRules(rules, reads("rules"));
    ResilientRecipeTemplates resilientTemplates =
        new ResilientRecipeTemplates(templates, reads("templates"));
    ResilientFoodPrices resilientPrices = new ResilientFoodPrices(prices, reads("prices"));
    HouseholdId home = HouseholdId.newId();
    Currency cop = Currency.getInstance("COP");
    resilientRules.all();
    resilientTemplates.templates();
    resilientTemplates.template(soup.id());
    resilientPrices.pricesFor(home, cop);
    failuresLeft = 100;

    assertEquals(List.of(), resilientRules.all());
    assertEquals(List.of(soup), resilientTemplates.templates());
    assertEquals(Optional.of(soup), resilientTemplates.template(soup.id()));
    assertEquals(Map.of("rice", BigDecimal.TEN), resilientPrices.pricesFor(home, cop));
    assertEquals(3, health.current().size());
    failuresLeft = 0;
    resilientRules.save(null);
    resilientPrices.save(home, "rice", BigDecimal.ONE);
    assertEquals(1, saved.size());
  }

  @Test
  void retryPoliciesAreValidatedAndRespectInterrupts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RetryPolicy(0, Duration.ZERO, 1, RetryPolicy.THREAD_SLEEP));
    RetryPolicy interrupted =
        new RetryPolicy(
            2,
            Duration.ofMillis(1),
            1,
            duration -> {
              throw new InterruptedException();
            });
    assertThrows(
        IllegalStateException.class,
        () ->
            interrupted.run(
                () -> {
                  throw new RuntimeException("down");
                }));
    assertTrue(Thread.interrupted());
    assertEquals(
        "ok", new RetryPolicy(1, Duration.ZERO, 1, RetryPolicy.THREAD_SLEEP).run(() -> "ok"));
  }
}
