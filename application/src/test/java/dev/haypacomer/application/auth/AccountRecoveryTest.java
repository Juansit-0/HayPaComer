package dev.haypacomer.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.identity.RefreshToken;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountRecoveryTest {

  private final AuthFakes fakes = new AuthFakes();
  private User juan;

  @BeforeEach
  void registerJuan() {
    juan =
        fakes
            .registerUser()
            .register(new RegisterCommand("juan@haypacomer.dev", "fresh-milk-842", "Juan"));
  }

  @Test
  void verifiesEmailWithASingleUseLink() {
    fakes.requestEmailVerification().request(juan.id());

    assertEquals(1, fakes.outbox.sent.size());
    assertEquals(juan.email(), fakes.outbox.sent.getFirst().to());
    assertTrue(
        fakes
            .outbox
            .sent
            .getFirst()
            .link()
            .startsWith("https://haypacomer.dev/verify-email#token="));
    String token = fakes.outbox.lastToken();

    assertTrue(fakes.verifyEmail().verify(token).emailVerified());
    assertTrue(fakes.users.findById(juan.id()).orElseThrow().emailVerified());
    assertThrows(InvalidTokenException.class, () -> fakes.verifyEmail().verify(token));
  }

  @Test
  void skipsVerifiedUsersAndRejectsUnknownOnes() {
    fakes.users.save(juan.verifyEmail());

    fakes.requestEmailVerification().request(juan.id());

    assertTrue(fakes.outbox.sent.isEmpty());
    assertThrows(
        UserNotFoundException.class,
        () -> fakes.requestEmailVerification().request(UserId.newId()));
  }

  @Test
  void verificationLinksExpireAfterADay() {
    fakes.requestEmailVerification().request(juan.id());
    fakes.clock.advance(Duration.ofHours(25));

    assertThrows(
        InvalidTokenException.class, () -> fakes.verifyEmail().verify(fakes.outbox.lastToken()));
    assertThrows(InvalidTokenException.class, () -> fakes.verifyEmail().verify(" "));
    assertThrows(InvalidTokenException.class, () -> fakes.verifyEmail().verify("unknown"));
  }

  @Test
  void passwordResetChangesThePasswordAndEndsEverySession() {
    AuthTokens session =
        fakes.logIn().logIn(new LoginCommand("juan@haypacomer.dev", "fresh-milk-842"));

    fakes.requestPasswordReset().request("JUAN@haypacomer.dev");
    fakes.resetPassword().reset(fakes.outbox.lastToken(), "new-password-123");

    assertThrows(
        InvalidCredentialsException.class,
        () -> fakes.logIn().logIn(new LoginCommand("juan@haypacomer.dev", "fresh-milk-842")));
    assertEquals(
        juan.id(),
        fakes.logIn().logIn(new LoginCommand("juan@haypacomer.dev", "new-password-123")).user());
    RefreshToken old =
        fakes
            .refreshTokens
            .findByHash(fakes.opaqueTokens.hash(session.refreshToken()))
            .orElseThrow();
    assertTrue(old.isRevoked());
  }

  @Test
  void resetRequestsNeverRevealWhetherAnAccountExists() {
    fakes.requestPasswordReset().request("ghost@haypacomer.dev");
    fakes.requestPasswordReset().request("not-an-email");
    fakes.requestPasswordReset().request(null);
    fakes.users.save(juan.disable());
    fakes.requestPasswordReset().request("juan@haypacomer.dev");

    assertTrue(fakes.outbox.sent.isEmpty());
  }

  @Test
  void weakPasswordIsRejectedWithoutBurningTheLink() {
    fakes.requestPasswordReset().request("juan@haypacomer.dev");
    String token = fakes.outbox.lastToken();

    assertThrows(InvalidPasswordException.class, () -> fakes.resetPassword().reset(token, "short"));
    fakes.resetPassword().reset(token, "new-password-123");
    assertThrows(
        InvalidTokenException.class, () -> fakes.resetPassword().reset(token, "other-password-1"));
  }

  @Test
  void resetLinksExpireAfterAnHour() {
    fakes.requestPasswordReset().request("juan@haypacomer.dev");
    fakes.clock.advance(Duration.ofMinutes(61));

    assertThrows(
        InvalidTokenException.class,
        () -> fakes.resetPassword().reset(fakes.outbox.lastToken(), "new-password-123"));
    assertFalse(fakes.outbox.sent.getFirst().toString().contains("token"));
  }
}
