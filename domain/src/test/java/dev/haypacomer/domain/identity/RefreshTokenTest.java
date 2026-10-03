package dev.haypacomer.domain.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
  private final UserId user = UserId.newId();
  private final UUID family = UUID.randomUUID();

  private RefreshToken issue(String hash) {
    return RefreshToken.issue(user, hash, family, NOW, Duration.ofDays(30));
  }

  @Test
  void isUsableUntilItExpires() {
    RefreshToken token = issue("a");

    assertTrue(token.isUsable(NOW));
    assertFalse(token.isUsable(NOW.plus(Duration.ofDays(30))));
    assertTrue(token.isExpired(NOW.plus(Duration.ofDays(31))));
    assertTrue(token.successor().isEmpty());
  }

  @Test
  void revokeIsIdempotent() {
    RefreshToken revoked = issue("a").revoke(NOW);

    assertTrue(revoked.isRevoked());
    assertSame(revoked, revoked.revoke(NOW.plusSeconds(5)));
    assertFalse(revoked.isUsable(NOW));
  }

  @Test
  void rotationLinksToTheSuccessor() {
    RefreshToken first = issue("a");
    RefreshToken second = issue("b");

    RefreshToken rotated = first.rotateTo(second, NOW);

    assertEquals(second.id(), rotated.successor().orElseThrow());
    assertTrue(rotated.isRevoked());
    RefreshToken stranger =
        RefreshToken.issue(user, "c", UUID.randomUUID(), NOW, Duration.ofDays(1));
    assertThrows(IllegalArgumentException.class, () -> first.rotateTo(stranger, NOW));
  }

  @Test
  void mustExpireAfterIssue() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RefreshToken.issue(user, "a", family, NOW, Duration.ZERO));
  }
}
