package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.i18n.LocaleOption;
import dev.haypacomer.application.port.TranslationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

public class PostgresTranslationRepository implements TranslationRepository {

  private record Cached<T>(T value, Instant loadedAt) {}

  private final JdbcClient jdbc;
  private final Clock clock;
  private final Duration timeToLive;
  private final Map<String, Cached<?>> cache = new ConcurrentHashMap<>();

  public PostgresTranslationRepository(DataSource dataSource, Clock clock, Duration timeToLive) {
    this.jdbc = JdbcClient.create(dataSource);
    this.clock = clock;
    this.timeToLive = timeToLive;
  }

  @Override
  public List<LocaleOption> locales() {
    return cached(
        "locales",
        () ->
            jdbc.sql("SELECT code, name, is_default FROM locales ORDER BY is_default DESC, code")
                .query(
                    (row, number) ->
                        new LocaleOption(
                            row.getString("code"),
                            row.getString("name"),
                            row.getBoolean("is_default")))
                .list());
  }

  @Override
  public Map<String, String> texts(String locale) {
    return cached(
        "texts:" + locale,
        () -> pairs("SELECT key, text FROM translations WHERE locale = :locale", locale));
  }

  @Override
  public Map<String, String> templates(String locale) {
    return cached(
        "templates:" + locale,
        () -> pairs("SELECT source, target FROM message_templates WHERE locale = :locale", locale));
  }

  private Map<String, String> pairs(String sql, String locale) {
    return jdbc
        .sql(sql)
        .param("locale", locale)
        .query((row, number) -> Map.entry(row.getString(1), row.getString(2)))
        .list()
        .stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
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
