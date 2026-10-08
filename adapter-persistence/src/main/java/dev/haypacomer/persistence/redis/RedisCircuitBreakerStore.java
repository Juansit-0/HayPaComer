package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitPhase;
import dev.haypacomer.application.ai.CircuitState;
import dev.haypacomer.application.port.CircuitBreakerStore;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisCircuitBreakerStore implements CircuitBreakerStore {

  private static final String PREFIX = "ai:cb:";

  private final StringRedisTemplate redis;

  public RedisCircuitBreakerStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public CircuitState load(AdvisorSource provider) {
    Map<Object, Object> fields = redis.opsForHash().entries(key(provider));
    if (fields.isEmpty()) {
      return CircuitState.CLOSED;
    }
    Object openedAt = fields.get("openedAt");
    return new CircuitState(
        CircuitPhase.valueOf((String) fields.get("phase")),
        Integer.parseInt((String) fields.get("failures")),
        openedAt == null ? null : Instant.parse((String) openedAt));
  }

  @Override
  public void save(AdvisorSource provider, CircuitState state) {
    Map<String, String> fields = new HashMap<>();
    fields.put("phase", state.phase().name());
    fields.put("failures", Integer.toString(state.failures()));
    String key = key(provider);
    redis.delete(key);
    if (state.openedAt() != null) {
      fields.put("openedAt", state.openedAt().toString());
    }
    redis.opsForHash().putAll(key, fields);
  }

  private static String key(AdvisorSource provider) {
    return PREFIX + provider.name().toLowerCase(Locale.ROOT);
  }
}
