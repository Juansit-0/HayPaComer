package dev.haypacomer.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.identity.RefreshToken;
import dev.haypacomer.domain.identity.User;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionUseCasesTest {

  private static final String PASSWORD = "fresh-milk-842";

  private final AuthFakes fakes = new AuthFakes();
  private final LogIn logIn = fakes.logIn();
  private final RefreshSession refreshSession = fakes.refreshSession();
  private final LogOut logOut = fakes.logOut();
  private User juan;

  @BeforeEach
  void registerJuan() {
    juan =
        fakes.registerUser().register(new RegisterCommand("juan@haypacomer.dev", PASSWORD, "Juan"));
  }

  private AuthTokens login() {
    return logIn.logIn(new LoginCommand("JUAN@haypacomer.dev", PASSWORD));
  }

  @Test
  void loginIssuesAccessAndRefreshTokens() {
    AuthTokens tokens = login();

    assertEquals(juan.id(), tokens.user());
    assertEquals("access-" + juan.id().value(), tokens.accessToken().value());
    assertEquals(fakes.clock.instant().plus(Duration.ofDays(30)), tokens.refreshExpiresAt());
    RefreshToken stored =
        fakes
            .refreshTokens
            .findByHash(fakes.opaqueTokens.hash(tokens.refreshToken()))
            .orElseThrow();
    assertTrue(stored.isUsable(fakes.clock.instant()));
    assertFalse(tokens.toString().contains(tokens.refreshToken()));
    assertFalse(tokens.accessToken().toString().contains("access-"));
  }

  @Test
  void wrongPasswordUnknownEmailAndDisabledUserLookTheSame() {
    assertThrows(
        InvalidCredentialsException.class,
        () -> logIn.logIn(new LoginCommand("juan@haypacomer.dev", "wrong-password")));
    int hashesBefore = fakes.hasher.hashes;
    assertThrows(
        InvalidCredentialsException.class,
        () -> logIn.logIn(new LoginCommand("ghost@haypacomer.dev", PASSWORD)));
    assertEquals(hashesBefore + 1, fakes.hasher.hashes);
    assertThrows(
        InvalidCredentialsException.class, () -> logIn.logIn(new LoginCommand("not-email", "x")));
    assertThrows(
        InvalidCredentialsException.class,
        () -> logIn.logIn(new LoginCommand("juan@haypacomer.dev", null)));
    fakes.users.save(juan.disable());
    assertThrows(InvalidCredentialsException.class, this::login);
  }

  @Test
  void locksOutAfterRepeatedFailuresWithinTheWindow() {
    for (int attempt = 0; attempt < 5; attempt++) {
      assertThrows(
          InvalidCredentialsException.class,
          () -> logIn.logIn(new LoginCommand("juan@haypacomer.dev", "wrong-password")));
    }

    assertThrows(TooManyLoginAttemptsException.class, this::login);

    fakes.clock.advance(Duration.ofMinutes(16));
    assertEquals(juan.id(), login().user());
  }

  @Test
  void refreshRotatesWithinTheSameFamily() {
    AuthTokens first = login();
    fakes.clock.advance(Duration.ofMinutes(20));

    AuthTokens second = refreshSession.refresh(first.refreshToken());

    assertNotEquals(first.refreshToken(), second.refreshToken());
    RefreshToken old =
        fakes.refreshTokens.findByHash(fakes.opaqueTokens.hash(first.refreshToken())).orElseThrow();
    RefreshToken current =
        fakes
            .refreshTokens
            .findByHash(fakes.opaqueTokens.hash(second.refreshToken()))
            .orElseThrow();
    assertTrue(old.isRevoked());
    assertEquals(current.id(), old.successor().orElseThrow());
    assertEquals(old.family(), current.family());
  }

  @Test
  void reusingARotatedTokenRevokesTheWholeFamily() {
    AuthTokens first = login();
    AuthTokens second = refreshSession.refresh(first.refreshToken());

    assertThrows(
        RefreshTokenReuseException.class, () -> refreshSession.refresh(first.refreshToken()));

    assertThrows(
        RefreshTokenReuseException.class, () -> refreshSession.refresh(second.refreshToken()));
    RefreshToken any =
        fakes
            .refreshTokens
            .findByHash(fakes.opaqueTokens.hash(second.refreshToken()))
            .orElseThrow();
    assertTrue(fakes.refreshTokens.family(any.family()).stream().allMatch(RefreshToken::isRevoked));
  }

  @Test
  void refreshRejectsUnknownExpiredAndDisabled() {
    assertThrows(InvalidCredentialsException.class, () -> refreshSession.refresh("unknown"));
    assertThrows(InvalidCredentialsException.class, () -> refreshSession.refresh(" "));
    assertThrows(InvalidCredentialsException.class, () -> refreshSession.refresh(null));

    AuthTokens expiring = login();
    fakes.clock.advance(Duration.ofDays(31));
    assertThrows(
        InvalidCredentialsException.class, () -> refreshSession.refresh(expiring.refreshToken()));

    AuthTokens fresh = login();
    fakes.users.save(juan.disable());
    assertThrows(
        InvalidCredentialsException.class, () -> refreshSession.refresh(fresh.refreshToken()));
  }

  @Test
  void logoutRevokesTheSessionAndIsIdempotent() {
    AuthTokens tokens = login();

    logOut.logOut(tokens.refreshToken());
    logOut.logOut(tokens.refreshToken());
    logOut.logOut("unknown");
    logOut.logOut(null);
    logOut.logOut(" ");

    assertThrows(
        InvalidCredentialsException.class, () -> refreshSession.refresh(tokens.refreshToken()));
  }
}
