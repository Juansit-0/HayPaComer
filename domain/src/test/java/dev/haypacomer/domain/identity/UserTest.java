package dev.haypacomer.domain.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  private static User juan() {
    return User.register(
        new EmailAddress("Juan@HayPaComer.dev"), new PasswordHash("$2a$12$hash"), " Juan ", NOW);
  }

  @Test
  void registersUnverifiedAndEnabled() {
    User user = juan();

    assertEquals("juan@haypacomer.dev", user.email().value());
    assertEquals("Juan", user.displayName());
    assertFalse(user.emailVerified());
    assertTrue(user.canSignIn());
    assertEquals(NOW, user.createdAt());
  }

  @Test
  void changesStateImmutably() {
    User user = juan();

    assertTrue(user.verifyEmail().emailVerified());
    assertEquals("Juanca", user.rename("Juanca").displayName());
    assertEquals(
        new PasswordHash("new"), user.changePassword(new PasswordHash("new")).passwordHash());
    assertFalse(user.disable().canSignIn());
    assertFalse(user.emailVerified());
  }

  @Test
  void validatesEmail() {
    assertEquals("ana@mail.co", new EmailAddress(" ANA@mail.co ").toString());
    assertThrows(IllegalArgumentException.class, () -> new EmailAddress("not-an-email"));
    assertThrows(IllegalArgumentException.class, () -> new EmailAddress("a@b"));
    assertThrows(IllegalArgumentException.class, () -> new EmailAddress("a b@c.com"));
    assertThrows(IllegalArgumentException.class, () -> new EmailAddress("a".repeat(250) + "@x.co"));
    assertThrows(NullPointerException.class, () -> new EmailAddress(null));
  }

  @Test
  void passwordHashNeverPrintsItsValue() {
    PasswordHash hash = new PasswordHash("$2a$12$secret");

    assertFalse(hash.toString().contains("secret"));
    assertThrows(IllegalArgumentException.class, () -> new PasswordHash(" "));
  }

  @Test
  void rejectsBlankDisplayName() {
    assertThrows(IllegalArgumentException.class, () -> juan().rename("  "));
    assertThrows(NullPointerException.class, () -> new UserId(null));
  }
}
