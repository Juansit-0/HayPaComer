package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.RefreshToken;
import dev.haypacomer.domain.identity.User;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresSessionStoresTest extends PostgresTestSupport {

  private PostgresRefreshTokenStore tokens;
  private PostgresLoginAttemptLog attempts;
  private User juan;

  @BeforeEach
  void createData() {
    juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    tokens = new PostgresRefreshTokenStore(dataSource);
    attempts = new PostgresLoginAttemptLog(dataSource);
  }

  private static String hash(int seed) {
    byte[] bytes = new byte[32];
    bytes[0] = (byte) seed;
    return HexFormat.of().formatHex(bytes);
  }

  @Test
  void roundTripsAndRotatesRefreshTokens() {
    UUID family = UUID.randomUUID();
    RefreshToken first = RefreshToken.issue(juan.id(), hash(1), family, NOW, Duration.ofDays(30));
    RefreshToken second = RefreshToken.issue(juan.id(), hash(2), family, NOW, Duration.ofDays(30));
    tokens.save(first);
    tokens.save(second);

    assertEquals(first, tokens.findByHash(hash(1)).orElseThrow());

    tokens.save(first.rotateTo(second, NOW.plusSeconds(60)));

    RefreshToken rotated = tokens.findByHash(hash(1)).orElseThrow();
    assertEquals(second.id(), rotated.successor().orElseThrow());
    assertEquals(NOW.plusSeconds(60), rotated.revokedAt());
    assertTrue(tokens.findByHash(hash(9)).isEmpty());
  }

  @Test
  void revokesOnlyTheGivenFamily() {
    UUID family = UUID.randomUUID();
    tokens.save(RefreshToken.issue(juan.id(), hash(1), family, NOW, Duration.ofDays(30)));
    tokens.save(RefreshToken.issue(juan.id(), hash(2), family, NOW, Duration.ofDays(30)));
    tokens.save(
        RefreshToken.issue(juan.id(), hash(3), UUID.randomUUID(), NOW, Duration.ofDays(30)));

    tokens.revokeFamily(family, NOW);

    assertTrue(tokens.findByHash(hash(1)).orElseThrow().isRevoked());
    assertTrue(tokens.findByHash(hash(2)).orElseThrow().isRevoked());
    assertTrue(tokens.findByHash(hash(3)).orElseThrow().isUsable(NOW));
  }

  @Test
  void countsRecentFailuresPerEmail() {
    EmailAddress email = juan.email();
    attempts.record(email, false, NOW.minus(Duration.ofMinutes(30)));
    attempts.record(email, false, NOW.minus(Duration.ofMinutes(5)));
    attempts.record(email, true, NOW.minus(Duration.ofMinutes(4)));
    attempts.record(new EmailAddress("ghost@haypacomer.dev"), false, NOW);

    assertEquals(1, attempts.failuresSince(email, NOW.minus(Duration.ofMinutes(15))));
    assertEquals(2, attempts.failuresSince(email, NOW.minus(Duration.ofHours(1))));
    assertEquals(
        1,
        attempts.failuresSince(
            new EmailAddress("ghost@haypacomer.dev"), NOW.minus(Duration.ofMinutes(1))));
  }
}
