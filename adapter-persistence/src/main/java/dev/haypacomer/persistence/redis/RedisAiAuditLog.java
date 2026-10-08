package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.port.AiAuditLog;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisAiAuditLog implements AiAuditLog {

  static final String KEY = "ai:audit";
  static final long MAX_ENTRIES = 100_000;

  private final StringRedisTemplate redis;

  public RedisAiAuditLog(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public void record(AiAuditEntry entry) {
    redis
        .opsForStream()
        .add(
            StreamRecords.newRecord()
                .in(KEY)
                .ofMap(
                    Map.of(
                        "provider", entry.provider(),
                        "operation", entry.operation(),
                        "latencyMs", Long.toString(entry.latency().toMillis()),
                        "outcome", entry.outcome().name(),
                        "at", entry.at().toString())));
    redis.opsForStream().trim(KEY, MAX_ENTRIES, true);
  }

  @Override
  public List<AiAuditEntry> recent(int limit) {
    var records =
        redis.opsForStream().reverseRange(KEY, Range.unbounded(), Limit.limit().count(limit));
    if (records == null) {
      return List.of();
    }
    return records.stream()
        .map(
            record ->
                new AiAuditEntry(
                    (String) record.getValue().get("provider"),
                    (String) record.getValue().get("operation"),
                    Duration.ofMillis(Long.parseLong((String) record.getValue().get("latencyMs"))),
                    AiOutcome.valueOf((String) record.getValue().get("outcome")),
                    Instant.parse((String) record.getValue().get("at"))))
        .toList();
  }
}
