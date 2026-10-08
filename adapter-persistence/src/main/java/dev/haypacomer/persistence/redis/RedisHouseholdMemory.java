package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.port.HouseholdMemory;
import dev.haypacomer.domain.household.HouseholdId;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisHouseholdMemory implements HouseholdMemory {

  static final int MAX_KEY = 80;
  static final int MAX_VALUE = 500;

  private final StringRedisTemplate redis;

  public RedisHouseholdMemory(StringRedisTemplate redis) {
    this.redis = redis;
  }

  private static String key(HouseholdId household) {
    return "agent:memory:" + household.value();
  }

  @Override
  public Map<String, String> read(HouseholdId household) {
    Map<String, String> memory = new TreeMap<>();
    redis
        .opsForHash()
        .entries(key(household))
        .forEach((k, v) -> memory.put((String) k, (String) v));
    return memory;
  }

  @Override
  public void remember(HouseholdId household, String key, String value) {
    if (key.isBlank() || key.length() > MAX_KEY || value.isBlank() || value.length() > MAX_VALUE) {
      throw new IllegalArgumentException("Memory keeps short, non-blank keys and values");
    }
    redis.opsForHash().put(key(household), key.strip(), value.strip());
  }

  @Override
  public void forget(HouseholdId household, String key) {
    redis.opsForHash().delete(key(household), key);
  }
}
