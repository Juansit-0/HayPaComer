package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.port.AiRateLimiter;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisAiRateLimiter implements AiRateLimiter {

  private static final String PREFIX = "ai:ratelimit:";

  private final StringRedisTemplate redis;

  public RedisAiRateLimiter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public boolean tryAcquire(String subject, int limit, Duration window) {
    if (limit < 1) {
      throw new IllegalArgumentException("Limit must be at least 1");
    }
    String key = PREFIX + subject;
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1) {
      redis.expire(key, window);
    }
    return count != null && count <= limit;
  }
}
