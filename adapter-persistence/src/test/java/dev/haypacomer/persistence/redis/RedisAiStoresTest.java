package dev.haypacomer.persistence.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitPhase;
import dev.haypacomer.application.ai.CircuitState;
import java.time.Duration;
import java.time.Instant;
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
class RedisAiStoresTest {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

  private static LettuceConnectionFactory connections;
  private static StringRedisTemplate redis;

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
  void cachesValidatedAnswersWithATimeToLive() {
    RedisAiResponseCache cache = new RedisAiResponseCache(redis);

    assertTrue(cache.get("abc").isEmpty());
    cache.put("abc", "{\"ok\":true}", Duration.ofMinutes(5));

    assertEquals("{\"ok\":true}", cache.get("abc").orElseThrow());
    long ttl = redis.getExpire("ai:cache:abc");
    assertTrue(ttl > 250 && ttl <= 300);
  }

  @Test
  void keepsTheBreakerStatePerProvider() {
    RedisCircuitBreakerStore store = new RedisCircuitBreakerStore(redis);
    Instant opened = Instant.parse("2026-10-08T20:00:00Z");

    assertEquals(CircuitState.CLOSED, store.load(AdvisorSource.GEMINI));
    store.save(AdvisorSource.GEMINI, new CircuitState(CircuitPhase.OPEN, 3, opened));
    store.save(AdvisorSource.OPENAI_COMPATIBLE, new CircuitState(CircuitPhase.CLOSED, 1, null));

    assertEquals(new CircuitState(CircuitPhase.OPEN, 3, opened), store.load(AdvisorSource.GEMINI));
    assertEquals(1, store.load(AdvisorSource.OPENAI_COMPATIBLE).failures());
    store.save(AdvisorSource.GEMINI, CircuitState.CLOSED);
    assertEquals(CircuitState.CLOSED, store.load(AdvisorSource.GEMINI));
  }

  @Test
  void limitsCallsPerSubjectAndWindow() {
    RedisAiRateLimiter limiter = new RedisAiRateLimiter(redis);

    assertTrue(limiter.tryAcquire("user-1", 2, Duration.ofMinutes(1)));
    assertTrue(limiter.tryAcquire("user-1", 2, Duration.ofMinutes(1)));
    assertFalse(limiter.tryAcquire("user-1", 2, Duration.ofMinutes(1)));
    assertTrue(limiter.tryAcquire("user-2", 2, Duration.ofMinutes(1)));
    assertTrue(redis.getExpire("ai:ratelimit:user-1") > 0);
    assertThrows(
        IllegalArgumentException.class,
        () -> limiter.tryAcquire("user-1", 0, Duration.ofMinutes(1)));
  }
}
