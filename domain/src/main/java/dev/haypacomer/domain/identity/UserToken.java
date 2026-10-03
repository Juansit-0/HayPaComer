package dev.haypacomer.domain.identity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserToken(
    UUID id, UserId user, UserTokenType type, String tokenHash, Instant expiresAt, Instant usedAt) {

  public UserToken {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(tokenHash, "tokenHash");
    Objects.requireNonNull(expiresAt, "expiresAt");
  }

  public static UserToken issue(UserId user, UserTokenType type, String tokenHash, Instant now) {
    return new UserToken(
        UUID.randomUUID(), user, type, tokenHash, now.plus(type.timeToLive()), null);
  }

  public boolean isUsable(UserTokenType expected, Instant now) {
    return type == expected && usedAt == null && now.isBefore(expiresAt);
  }

  public UserToken use(Instant at) {
    if (usedAt != null) {
      throw new IllegalStateException("Token already used");
    }
    return new UserToken(id, user, type, tokenHash, expiresAt, at);
  }
}
