package dev.haypacomer.persistence.redis;

import dev.haypacomer.application.agent.ChatMessage;
import dev.haypacomer.application.agent.ChatRole;
import dev.haypacomer.application.agent.ConversationId;
import dev.haypacomer.application.agent.ConversationSummary;
import dev.haypacomer.application.port.ConversationStore;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisConversationStore implements ConversationStore {

  static final Duration TIME_TO_LIVE = Duration.ofDays(7);

  private final StringRedisTemplate redis;

  public RedisConversationStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  private static String stream(ConversationId conversation) {
    return "agent:conv:" + conversation.value();
  }

  private static String index(UserId user) {
    return "agent:convs:" + user.value();
  }

  @Override
  public void append(UserId user, ConversationId conversation, ChatMessage message) {
    redis
        .opsForStream()
        .add(
            StreamRecords.newRecord()
                .in(stream(conversation))
                .ofMap(
                    Map.of(
                        "role", message.role().name(),
                        "text", message.text(),
                        "at", message.at().toString())));
    redis.expire(stream(conversation), TIME_TO_LIVE);
    redis
        .opsForZSet()
        .add(index(user), conversation.value().toString(), message.at().toEpochMilli());
    redis.expire(index(user), TIME_TO_LIVE);
  }

  @Override
  public List<ChatMessage> messages(ConversationId conversation, int limit) {
    List<MapRecord<String, Object, Object>> records =
        redis
            .opsForStream()
            .reverseRange(stream(conversation), Range.unbounded(), Limit.limit().count(limit));
    List<ChatMessage> messages = new ArrayList<>();
    if (records != null) {
      records.forEach(
          record ->
              messages.add(
                  new ChatMessage(
                      ChatRole.valueOf((String) record.getValue().get("role")),
                      (String) record.getValue().get("text"),
                      Instant.parse((String) record.getValue().get("at")))));
    }
    Collections.reverse(messages);
    return messages;
  }

  @Override
  public List<ConversationSummary> recent(UserId user, int limit) {
    var entries = redis.opsForZSet().reverseRangeWithScores(index(user), 0, limit - 1L);
    if (entries == null) {
      return List.of();
    }
    return entries.stream()
        .map(
            entry ->
                new ConversationSummary(
                    new ConversationId(UUID.fromString(Objects.requireNonNull(entry.getValue()))),
                    Instant.ofEpochMilli(Objects.requireNonNull(entry.getScore()).longValue())))
        .toList();
  }
}
