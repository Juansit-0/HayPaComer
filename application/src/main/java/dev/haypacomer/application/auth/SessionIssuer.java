package dev.haypacomer.application.auth;

import dev.haypacomer.application.port.AccessTokenIssuer;
import dev.haypacomer.application.port.RefreshTokenStore;
import dev.haypacomer.domain.identity.RefreshToken;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class SessionIssuer {

  private final AccessTokenIssuer accessTokens;
  private final RefreshTokenStore refreshTokens;
  private final OpaqueTokens opaqueTokens;
  private final AuthSettings settings;

  public SessionIssuer(
      AccessTokenIssuer accessTokens,
      RefreshTokenStore refreshTokens,
      OpaqueTokens opaqueTokens,
      AuthSettings settings) {
    this.accessTokens = Objects.requireNonNull(accessTokens, "accessTokens");
    this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
    this.opaqueTokens = Objects.requireNonNull(opaqueTokens, "opaqueTokens");
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  Issued issue(UserId user, UUID family, Instant now) {
    String rawRefreshToken = opaqueTokens.generate();
    RefreshToken refreshToken =
        RefreshToken.issue(
            user,
            opaqueTokens.hash(rawRefreshToken),
            family,
            now,
            settings.refreshTokenTimeToLive());
    refreshTokens.save(refreshToken);
    AuthTokens tokens =
        new AuthTokens(
            user, accessTokens.issue(user, now), rawRefreshToken, refreshToken.expiresAt());
    return new Issued(tokens, refreshToken);
  }

  record Issued(AuthTokens tokens, RefreshToken refreshToken) {}
}
