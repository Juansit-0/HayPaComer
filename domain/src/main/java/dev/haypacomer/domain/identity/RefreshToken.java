package dev.haypacomer.domain.identity;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record RefreshToken(
    UUID id,
    UserId user,
    String tokenHash,
    UUID family,
    Instant issuedAt,
    Instant expiresAt,
    Instant revokedAt,
    UUID replacedBy) {

  public RefreshToken {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(tokenHash, "tokenHash");
    Objects.requireNonNull(family, "family");
    Objects.requireNonNull(issuedAt, "issuedAt");
    Objects.requireNonNull(expiresAt, "expiresAt");
    if (!expiresAt.isAfter(issuedAt)) {
      throw new IllegalArgumentException("Refresh token must expire after it is issued");
    }
  }

  public static RefreshToken issue(
      UserId user, String tokenHash, UUID family, Instant now, Duration timeToLive) {
    return new RefreshToken(
        UUID.randomUUID(), user, tokenHash, family, now, now.plus(timeToLive), null, null);
  }

  public boolean isRevoked() {
    return revokedAt != null;
  }

  public boolean isExpired(Instant now) {
    return !now.isBefore(expiresAt);
  }

  public boolean isUsable(Instant now) {
    return !isRevoked() && !isExpired(now);
  }

  public Optional<UUID> successor() {
    return Optional.ofNullable(replacedBy);
  }

  public RefreshToken revoke(Instant at) {
    return isRevoked()
        ? this
        : new RefreshToken(id, user, tokenHash, family, issuedAt, expiresAt, at, replacedBy);
  }

  public RefreshToken rotateTo(RefreshToken next, Instant at) {
    if (!next.family.equals(family) || !next.user.equals(user)) {
      throw new IllegalArgumentException("Rotation must stay in the same family and user");
    }
    return new RefreshToken(id, user, tokenHash, family, issuedAt, expiresAt, at, next.id);
  }
}
