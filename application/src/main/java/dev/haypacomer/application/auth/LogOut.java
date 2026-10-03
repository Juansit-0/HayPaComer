package dev.haypacomer.application.auth;

import dev.haypacomer.application.port.RefreshTokenStore;
import java.time.Clock;
import java.util.Objects;

public final class LogOut {

  private final RefreshTokenStore refreshTokens;
  private final OpaqueTokens opaqueTokens;
  private final Clock clock;

  public LogOut(RefreshTokenStore refreshTokens, OpaqueTokens opaqueTokens, Clock clock) {
    this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
    this.opaqueTokens = Objects.requireNonNull(opaqueTokens, "opaqueTokens");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public void logOut(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      return;
    }
    refreshTokens
        .findByHash(opaqueTokens.hash(rawRefreshToken))
        .ifPresent(token -> refreshTokens.revokeFamily(token.family(), clock.instant()));
  }
}
