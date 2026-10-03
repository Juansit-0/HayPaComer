package dev.haypacomer.domain.household;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InvitationTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  private Invitation invite() {
    return Invitation.create(
        HouseholdId.newId(),
        new EmailAddress("ana@haypacomer.dev"),
        Role.MEMBER,
        "hash",
        UserId.newId(),
        NOW);
  }

  @Test
  void isPendingForSevenDays() {
    Invitation invitation = invite();

    assertEquals(NOW.plus(Invitation.TIME_TO_LIVE), invitation.expiresAt());
    assertTrue(invitation.isPending(NOW));
    assertFalse(invitation.isPending(invitation.expiresAt()));
  }

  @Test
  void isAcceptedOnlyOnce() {
    Invitation accepted = invite().accept(NOW);

    assertFalse(accepted.isPending(NOW));
    assertThrows(IllegalStateException.class, () -> accepted.accept(NOW));
  }

  @Test
  void cannotGrantOwnership() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Invitation.create(
                HouseholdId.newId(),
                new EmailAddress("ana@haypacomer.dev"),
                Role.OWNER,
                "hash",
                UserId.newId(),
                NOW));
  }
}
