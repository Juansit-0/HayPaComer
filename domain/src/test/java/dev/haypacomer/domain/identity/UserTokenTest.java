package dev.haypacomer.domain.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTokenTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  @Test
  void expiresAccordingToItsType() {
    UserToken verify = UserToken.issue(UserId.newId(), UserTokenType.VERIFY_EMAIL, "a", NOW);
    UserToken reset = UserToken.issue(UserId.newId(), UserTokenType.RESET_PASSWORD, "b", NOW);

    assertEquals(NOW.plus(Duration.ofHours(24)), verify.expiresAt());
    assertTrue(reset.isUsable(UserTokenType.RESET_PASSWORD, NOW.plusSeconds(3599)));
    assertFalse(reset.isUsable(UserTokenType.RESET_PASSWORD, NOW.plusSeconds(3600)));
    assertFalse(reset.isUsable(UserTokenType.VERIFY_EMAIL, NOW));
  }

  @Test
  void canBeUsedOnlyOnce() {
    UserToken used = UserToken.issue(UserId.newId(), UserTokenType.VERIFY_EMAIL, "a", NOW).use(NOW);

    assertFalse(used.isUsable(UserTokenType.VERIFY_EMAIL, NOW));
    assertThrows(IllegalStateException.class, () -> used.use(NOW));
  }
}
