package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.port.AiResponseCache;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisAiResponseCache implements AiResponseCache {

  private static final String PREFIX = "ai:cache:";

  private final StringRedisTemplate redis;

  public RedisAiResponseCache(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Optional<String> get(String key) {
    return Optional.ofNullable(redis.opsForValue().get(PREFIX + key));
  }

  @Override
  public void put(String key, String json, Duration timeToLive) {
    redis.opsForValue().set(PREFIX + key, json, timeToLive);
  }
}
