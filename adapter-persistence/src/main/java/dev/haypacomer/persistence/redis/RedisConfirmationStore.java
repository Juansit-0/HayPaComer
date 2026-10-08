package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.port.ConfirmationStore;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

public final class RedisConfirmationStore implements ConfirmationStore {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, String>> ARGUMENTS = new TypeReference<>() {};

  private final StringRedisTemplate redis;
  private final Clock clock;

  public RedisConfirmationStore(StringRedisTemplate redis, Clock clock) {
    this.redis = redis;
    this.clock = clock;
  }

  private static String key(UUID id) {
    return "agent:pending:" + id;
  }

  private static String index(UserId user) {
    return "agent:pending:user:" + user.value();
  }

  @Override
  public void propose(PendingConfirmation confirmation) {
    Duration left = Duration.between(clock.instant(), confirmation.expiresAt());
    if (left.isNegative() || left.isZero()) {
      return;
    }
    Map<String, String> fields = new HashMap<>();
    fields.put("household", confirmation.household().value().toString());
    fields.put("user", confirmation.user().value().toString());
    fields.put("tool", confirmation.tool());
    fields.put("arguments", JSON.writeValueAsString(confirmation.arguments()));
    fields.put("summary", confirmation.summary());
    fields.put("proposedAt", confirmation.proposedAt().toString());
    fields.put("expiresAt", confirmation.expiresAt().toString());
    redis.opsForHash().putAll(key(confirmation.id()), fields);
    redis.expire(key(confirmation.id()), left);
    redis.opsForSet().add(index(confirmation.user()), confirmation.id().toString());
    redis.expire(index(confirmation.user()), PendingConfirmation.TIME_TO_LIVE);
  }

  @Override
  public Optional<PendingConfirmation> find(UUID id) {
    Map<Object, Object> fields = redis.opsForHash().entries(key(id));
    if (fields.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new PendingConfirmation(
            id,
            new HouseholdId(UUID.fromString((String) fields.get("household"))),
            new UserId(UUID.fromString((String) fields.get("user"))),
            (String) fields.get("tool"),
            JSON.readValue((String) fields.get("arguments"), ARGUMENTS),
            (String) fields.get("summary"),
            Instant.parse((String) fields.get("proposedAt")),
            Instant.parse((String) fields.get("expiresAt"))));
  }

  @Override
  public List<PendingConfirmation> pendingFor(UserId user) {
    Set<String> ids = redis.opsForSet().members(index(user));
    List<PendingConfirmation> pending = new ArrayList<>();
    if (ids == null) {
      return pending;
    }
    for (String id : ids) {
      Optional<PendingConfirmation> found = find(UUID.fromString(id));
      if (found.isPresent()) {
        pending.add(found.get());
      } else {
        redis.opsForSet().remove(index(user), id);
      }
    }
    pending.sort((a, b) -> a.proposedAt().compareTo(b.proposedAt()));
    return pending;
  }

  @Override
  public void remove(PendingConfirmation confirmation) {
    redis.delete(key(confirmation.id()));
    redis.opsForSet().remove(index(confirmation.user()), confirmation.id().toString());
  }
}
