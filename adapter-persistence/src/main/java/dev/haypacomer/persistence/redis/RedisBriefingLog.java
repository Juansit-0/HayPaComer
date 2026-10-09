package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.port.BriefingLog;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.Duration;
import java.time.LocalDate;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisBriefingLog implements BriefingLog {

  static final Duration KEEP = Duration.ofDays(2);

  private final StringRedisTemplate redis;

  public RedisBriefingLog(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public boolean claim(HouseholdId household, String kind, LocalDate day) {
    Boolean first =
        redis
            .opsForValue()
            .setIfAbsent(
                "agent:briefing:" + household.value() + ":" + kind + ":" + day, "sent", KEEP);
    return Boolean.TRUE.equals(first);
  }
}
