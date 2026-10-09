package dev.haypacomer.persistence.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.agent.AgentRun;
import dev.haypacomer.application.agent.AgentRunId;
import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.agent.ChatMessage;
import dev.haypacomer.application.agent.ChatRole;
import dev.haypacomer.application.agent.ConversationId;
import dev.haypacomer.application.agent.PendingConfirmation;
import dev.haypacomer.application.agent.RunStatus;
import dev.haypacomer.application.agent.TraceKind;
import dev.haypacomer.application.agent.TraceStep;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RedisAgentStoresTest {

  private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  private static LettuceConnectionFactory connections;
  private static StringRedisTemplate redis;

  private final HouseholdId household = HouseholdId.newId();
  private final UserId juan = UserId.newId();

  @BeforeAll
  static void connect() {
    connections =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
    connections.afterPropertiesSet();
    redis = new StringRedisTemplate(connections);
  }

  @AfterAll
  static void disconnect() {
    connections.destroy();
  }

  @BeforeEach
  void flush() {
    redis.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Test
  void householdMemoryIsEditable() {
    RedisHouseholdMemory memory = new RedisHouseholdMemory(redis);

    memory.remember(household, "usual rice", "150 g");
    memory.remember(household, "dislikes", " cilantro ");
    memory.forget(household, "usual rice");

    assertEquals(Map.of("dislikes", "cilantro"), memory.read(household));
    assertTrue(memory.read(HouseholdId.newId()).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> memory.remember(household, " ", "x"));
    assertThrows(
        IllegalArgumentException.class, () -> memory.remember(household, "k", "x".repeat(501)));
  }

  @Test
  void conversationsKeepOrderAndAnIndexByLastActivity() {
    RedisConversationStore store = new RedisConversationStore(redis);
    ConversationId dinner = ConversationId.newId();
    ConversationId market = ConversationId.newId();

    store.append(juan, dinner, new ChatMessage(ChatRole.USER, "What can I cook?", NOW));
    store.append(
        juan, dinner, new ChatMessage(ChatRole.ASSISTANT, "Rice with chicken", NOW.plusSeconds(2)));
    store.append(juan, market, new ChatMessage(ChatRole.USER, "Add milk", NOW.plusSeconds(60)));

    assertEquals(
        List.of("What can I cook?", "Rice with chicken"),
        store.messages(dinner, 10).stream().map(ChatMessage::text).toList());
    assertEquals(
        List.of("Rice with chicken"),
        store.messages(dinner, 1).stream().map(ChatMessage::text).toList());
    assertEquals(market, store.recent(juan, 10).getFirst().id());
    assertEquals(NOW.plusSeconds(60), store.recent(juan, 10).getFirst().lastActivity());
    assertEquals(2, store.recent(juan, 10).size());
    long ttl = redis.getExpire("agent:conv:" + dinner.value());
    assertTrue(ttl > Duration.ofDays(6).toSeconds());
    assertTrue(store.messages(ConversationId.newId(), 5).isEmpty());
  }

  @Test
  void runsAndTheirTracesAreKeptForThirtyDays() {
    RedisAgentRunStore store = new RedisAgentRunStore(redis);
    AgentRun run = AgentRun.start(household, juan, "chef", 8, NOW);

    store.save(run);
    store.trace(run.id(), new TraceStep(TraceKind.PLAN, "Check what expires first", NOW));
    store.trace(run.id(), new TraceStep(TraceKind.TOOL_CALL, "view_expiries", NOW.plusSeconds(1)));
    AgentRun done =
        new AgentRun(
            run.id(), household, juan, "chef", RunStatus.DONE, 2, 8, NOW, NOW.plusSeconds(3));
    store.save(done);

    assertEquals(done, store.find(run.id()).orElseThrow());
    assertEquals(
        List.of(TraceKind.PLAN, TraceKind.TOOL_CALL),
        store.traceOf(run.id()).stream().map(TraceStep::kind).toList());
    assertTrue(
        redis.getExpire("agent:trace:" + run.id().value()) > Duration.ofDays(29).toSeconds());
    assertTrue(store.find(AgentRunId.newId()).isEmpty());
  }

  @Test
  void confirmationsWaitTenMinutesAtMost() {
    RedisConfirmationStore store =
        new RedisConfirmationStore(redis, Clock.fixed(NOW, ZoneOffset.UTC));
    PendingConfirmation add =
        PendingConfirmation.propose(
            AgentRunId.newId(),
            household,
            juan,
            "add_to_market",
            Map.of("food", "Rice", "grams", "500"),
            "Add rice",
            NOW);
    PendingConfirmation stale =
        PendingConfirmation.propose(
            AgentRunId.newId(),
            household,
            juan,
            "add_to_market",
            Map.of(),
            "Old",
            NOW.minusSeconds(900));

    store.propose(add);
    store.propose(stale);

    assertEquals(add, store.find(add.id()).orElseThrow());
    assertEquals(List.of(add), store.pendingFor(juan));
    long ttl = redis.getExpire("agent:pending:" + add.id());
    assertTrue(ttl > 590 && ttl <= 600);
    redis.opsForSet().add("agent:pending:user:" + juan.value(), UUID.randomUUID().toString());
    assertEquals(1, store.pendingFor(juan).size());
    store.remove(add);
    assertTrue(store.find(add.id()).isEmpty());
    assertTrue(store.pendingFor(juan).isEmpty());
    assertTrue(store.pendingFor(UserId.newId()).isEmpty());
  }

  @Test
  void theAiAuditIsAnAppendOnlyStreamNewestFirst() {
    RedisAiAuditLog audit = new RedisAiAuditLog(redis);

    audit.record(
        new AiAuditEntry("gemini", "suggest", Duration.ofMillis(420), AiOutcome.VALID, NOW));
    audit.record(
        new AiAuditEntry(
            "gemini",
            "parseIntent",
            Duration.ofMillis(9000),
            AiOutcome.FALLBACK,
            NOW.plusSeconds(1)));

    List<AiAuditEntry> recent = audit.recent(10);
    assertEquals(AiOutcome.FALLBACK, recent.getFirst().outcome());
    assertEquals(Duration.ofMillis(420), recent.get(1).latency());
    assertEquals(1, audit.recent(1).size());
  }
}
