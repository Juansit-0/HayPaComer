package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.AgentRunId;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.agent.TraceKind;
import dev.haypacomer.application.agent.TraceStep;
import dev.haypacomer.application.port.AgentRunStore;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisAgentRunStore implements AgentRunStore {

  static final Duration TIME_TO_LIVE = Duration.ofDays(30);

  private final StringRedisTemplate redis;

  public RedisAgentRunStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  private static String run(AgentRunId id) {
    return "agent:run:" + id.value();
  }

  private static String trace(AgentRunId id) {
    return "agent:trace:" + id.value();
  }

  @Override
  public void save(AgentRun value) {
    Map<String, String> fields = new HashMap<>();
    fields.put("household", value.household().value().toString());
    fields.put("user", value.user().value().toString());
    fields.put("specialist", value.specialist());
    fields.put("status", value.status().name());
    fields.put("stepsUsed", Integer.toString(value.stepsUsed()));
    fields.put("stepBudget", Integer.toString(value.stepBudget()));
    fields.put("startedAt", value.startedAt().toString());
    value.finished().ifPresent(at -> fields.put("finishedAt", at.toString()));
    redis.opsForHash().putAll(run(value.id()), fields);
    redis.expire(run(value.id()), TIME_TO_LIVE);
  }

  @Override
  public Optional<AgentRun> find(AgentRunId id) {
    Map<Object, Object> fields = redis.opsForHash().entries(run(id));
    if (fields.isEmpty()) {
      return Optional.empty();
    }
    Object finished = fields.get("finishedAt");
    return Optional.of(
        new AgentRun(
            id,
            new HouseholdId(UUID.fromString((String) fields.get("household"))),
            new UserId(UUID.fromString((String) fields.get("user"))),
            (String) fields.get("specialist"),
            RunStatus.valueOf((String) fields.get("status")),
            Integer.parseInt((String) fields.get("stepsUsed")),
            Integer.parseInt((String) fields.get("stepBudget")),
            Instant.parse((String) fields.get("startedAt")),
            finished == null ? null : Instant.parse((String) finished)));
  }

  @Override
  public void trace(AgentRunId id, TraceStep step) {
    redis
        .opsForStream()
        .add(
            StreamRecords.newRecord()
                .in(trace(id))
                .ofMap(
                    Map.of(
                        "kind", step.kind().name(),
                        "detail", step.detail(),
                        "at", step.at().toString())));
    redis.expire(trace(id), TIME_TO_LIVE);
  }

  @Override
  public List<TraceStep> traceOf(AgentRunId id) {
    var records = redis.opsForStream().range(trace(id), Range.unbounded());
    if (records == null) {
      return List.of();
    }
    return records.stream()
        .map(
            record ->
                new TraceStep(
                    TraceKind.valueOf((String) record.getValue().get("kind")),
                    (String) record.getValue().get("detail"),
                    Instant.parse((String) record.getValue().get("at"))))
        .toList();
  }
}
