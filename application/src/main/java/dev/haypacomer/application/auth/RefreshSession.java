package dev.haypacomer.application.auth;

import dev.haypacomer.application.port.RefreshTokenStore;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.identity.RefreshToken;
import dev.haypacomer.domain.identity.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class RefreshSession {

  private final RefreshTokenStore refreshTokens;
  private final UserRepository users;
  private final SessionIssuer sessions;
  private final OpaqueTokens opaqueTokens;
  private final Clock clock;

  public RefreshSession(
      RefreshTokenStore refreshTokens,
      UserRepository users,
      SessionIssuer sessions,
      OpaqueTokens opaqueTokens,
      Clock clock) {
    this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
    this.users = Objects.requireNonNull(users, "users");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.opaqueTokens = Objects.requireNonNull(opaqueTokens, "opaqueTokens");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public AuthTokens refresh(String rawRefreshToken) {
    Instant now = clock.instant();
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new InvalidCredentialsException();
    }
    RefreshToken current =
        refreshTokens
            .findByHash(opaqueTokens.hash(rawRefreshToken))
            .orElseThrow(InvalidCredentialsException::new);
    if (current.isRevoked()) {
      refreshTokens.revokeFamily(current.family(), now);
      throw new RefreshTokenReuseException();
    }
    if (current.isExpired(now)) {
      throw new InvalidCredentialsException();
    }
    boolean active = users.findById(current.user()).map(User::canSignIn).orElse(false);
    if (!active) {
      refreshTokens.revokeFamily(current.family(), now);
      throw new InvalidCredentialsException();
    }
    SessionIssuer.Issued next = sessions.issue(current.user(), current.family(), now);
    refreshTokens.save(current.rotateTo(next.refreshToken(), now));
    return next.tokens();
  }
}
