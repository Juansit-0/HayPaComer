package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.SettingsRepository;
import dev.haypacomer.application.settings.SettingDefinition;
import dev.haypacomer.application.settings.SettingKind;
import dev.haypacomer.application.settings.SettingScope;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

public class PostgresSettingsRepository implements SettingsRepository {

  private record Cached<T>(T value, Instant loadedAt) {}

  private static final String DEFINITIONS = "definitions";

  private final JdbcClient jdbc;
  private final Clock clock;
  private final Duration timeToLive;
  private final Map<String, Cached<?>> cache = new ConcurrentHashMap<>();
  private final Map<FridgeId, HouseholdId> fridgeHouseholds = new ConcurrentHashMap<>();

  public PostgresSettingsRepository(DataSource dataSource, Clock clock, Duration timeToLive) {
    this.jdbc = JdbcClient.create(dataSource);
    this.clock = clock;
    this.timeToLive = timeToLive;
  }

  @Override
  public List<SettingDefinition> definitions() {
    return cached(
        DEFINITIONS,
        () ->
            jdbc.sql(
                    "SELECT key, kind, scope, value, min_value, max_value, description"
                        + " FROM settings ORDER BY key")
                .query(
                    (row, number) ->
                        new SettingDefinition(
                            row.getString("key"),
                            SettingKind.valueOf(row.getString("kind")),
                            SettingScope.valueOf(row.getString("scope")),
                            row.getString("value"),
                            row.getBigDecimal("min_value"),
                            row.getBigDecimal("max_value"),
                            row.getString("description")))
                .list());
  }

  @Override
  public Map<String, String> overrides(HouseholdId household) {
    return cached(
        household.value().toString(),
        () ->
            jdbc
                .sql("SELECT key, value FROM household_settings WHERE household_id = :household")
                .param("household", household.value())
                .query((row, number) -> Map.entry(row.getString("key"), row.getString("value")))
                .list()
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  @Override
  public void override(HouseholdId household, String key, String value) {
    jdbc.sql(
            """
            INSERT INTO household_settings (household_id, key, value, updated_at)
            VALUES (:household, :key, :value, now())
            ON CONFLICT (household_id, key)
            DO UPDATE SET value = EXCLUDED.value, updated_at = now()
            """)
        .param("household", household.value())
        .param("key", key)
        .param("value", value)
        .update();
    cache.remove(household.value().toString());
  }

  @Override
  public void reset(HouseholdId household, String key) {
    jdbc.sql("DELETE FROM household_settings WHERE household_id = :household AND key = :key")
        .param("household", household.value())
        .param("key", key)
        .update();
    cache.remove(household.value().toString());
  }

  @Override
  public Optional<HouseholdId> householdOf(FridgeId fridge) {
    HouseholdId known = fridgeHouseholds.get(fridge);
    if (known != null) {
      return Optional.of(known);
    }
    Optional<HouseholdId> found =
        jdbc.sql("SELECT household_id FROM fridges WHERE id = :fridge")
            .param("fridge", fridge.value())
            .query(UUID.class)
            .optional()
            .map(HouseholdId::new);
    found.ifPresent(household -> fridgeHouseholds.put(fridge, household));
    return found;
  }

  @SuppressWarnings("unchecked")
  private <T> T cached(String key, Supplier<T> load) {
    Instant now = clock.instant();
    Cached<?> entry = cache.get(key);
    if (entry != null && entry.loadedAt().plus(timeToLive).isAfter(now)) {
      return (T) entry.value();
    }
    try {
      T value = load.get();
      cache.put(key, new Cached<>(value, now));
      return value;
    } catch (RuntimeException failure) {
      if (entry == null) {
        throw failure;
      }
      return (T) entry.value();
    }
  }
}
