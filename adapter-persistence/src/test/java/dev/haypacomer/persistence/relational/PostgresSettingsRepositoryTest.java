package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.settings.SettingDefinition;
import dev.haypacomer.application.settings.SettingKind;
import dev.haypacomer.application.settings.SettingScope;
import dev.haypacomer.application.settings.StoredPolicies;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class PostgresSettingsRepositoryTest extends PostgresTestSupport {

  private static final class MovableClock extends Clock {

    private Instant now = NOW;

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
      return now;
    }
  }

  private Household household() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    return household;
  }

  @Test
  void seedsEverySettingWithTheCurrentDefaults() {
    PostgresSettingsRepository settings =
        new PostgresSettingsRepository(dataSource, Clock.systemUTC(), Duration.ofSeconds(30));

    assertEquals(18, settings.definitions().size());
    SettingDefinition celsius =
        settings.definitions().stream()
            .filter(definition -> definition.key().equals("fridge.max-celsius"))
            .findFirst()
            .orElseThrow();
    assertEquals(SettingKind.DECIMAL, celsius.kind());
    assertEquals(SettingScope.HOUSEHOLD, celsius.scope());
    assertEquals(0, new BigDecimal("10").compareTo(celsius.max()));
    StoredPolicies policies = new StoredPolicies(settings);
    assertEquals(FridgeThresholds.DEFAULT, policies.thresholds(null));
    assertEquals(FreshnessPolicy.DEFAULT, policies.freshness(null));
  }

  @Test
  void householdOverridesAreSavedResetAndCachedUntilWritten() {
    Household household = household();
    Fridge fridge = Fridge.named("Kitchen");
    new PostgresFridgeRepository(dataSource).save(household.id(), fridge);
    MovableClock clock = new MovableClock();
    PostgresSettingsRepository settings =
        new PostgresSettingsRepository(dataSource, clock, Duration.ofSeconds(30));

    assertTrue(settings.overrides(household.id()).isEmpty());
    settings.override(household.id(), "food.at-risk-days", "4");
    settings.override(household.id(), "food.at-risk-days", "5");
    assertEquals(Map.of("food.at-risk-days", "5"), settings.overrides(household.id()));
    assertEquals(household.id(), settings.householdOf(fridge.id()).orElseThrow());
    assertEquals(household.id(), settings.householdOf(fridge.id()).orElseThrow());
    assertTrue(settings.householdOf(FridgeId.newId()).isEmpty());

    JdbcClient.create(dataSource).sql("UPDATE household_settings SET value = '9'").update();
    assertEquals("5", settings.overrides(household.id()).get("food.at-risk-days"));
    clock.now = NOW.plusSeconds(31);
    assertEquals("9", settings.overrides(household.id()).get("food.at-risk-days"));

    settings.reset(household.id(), "food.at-risk-days");
    assertTrue(settings.overrides(household.id()).isEmpty());
  }

  @Test
  void servesTheLastCopyWhenTheDatabaseFails() {
    Household household = household();
    MovableClock clock = new MovableClock();
    PostgresSettingsRepository settings =
        new PostgresSettingsRepository(dataSource, clock, Duration.ofSeconds(30));
    settings.override(household.id(), "food.at-risk-days", "3");
    assertEquals(18, settings.definitions().size());
    assertEquals("3", settings.overrides(household.id()).get("food.at-risk-days"));

    JdbcClient.create(dataSource)
        .sql("ALTER TABLE household_settings RENAME TO household_settings_away")
        .update();
    JdbcClient.create(dataSource).sql("ALTER TABLE settings RENAME TO settings_away").update();
    try {
      clock.now = NOW.plusSeconds(60);
      assertEquals(18, settings.definitions().size());
      assertEquals("3", settings.overrides(household.id()).get("food.at-risk-days"));
    } finally {
      JdbcClient.create(dataSource).sql("ALTER TABLE settings_away RENAME TO settings").update();
      JdbcClient.create(dataSource)
          .sql("ALTER TABLE household_settings_away RENAME TO household_settings")
          .update();
    }
  }
}
