package dev.haypacomer.application.auth;

import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.Objects;

public record AuthTokens(
    UserId user, AccessToken accessToken, String refreshToken, Instant refreshExpiresAt) {

  public AuthTokens {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(accessToken, "accessToken");
    Objects.requireNonNull(refreshToken, "refreshToken");
    Objects.requireNonNull(refreshExpiresAt, "refreshExpiresAt");
  }

  @Override
  public String toString() {
    return "AuthTokens[user=" + user + ", refreshExpiresAt=" + refreshExpiresAt + "]";
  }
}
