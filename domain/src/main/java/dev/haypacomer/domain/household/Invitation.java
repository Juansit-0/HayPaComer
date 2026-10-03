package dev.haypacomer.domain.household;

import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Invitation(
    UUID id,
    HouseholdId household,
    EmailAddress email,
    Role role,
    String tokenHash,
    UserId invitedBy,
    Instant expiresAt,
    Instant acceptedAt) {

  public static final Duration TIME_TO_LIVE = Duration.ofDays(7);

  public Invitation {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(household, "household");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(tokenHash, "tokenHash");
    Objects.requireNonNull(invitedBy, "invitedBy");
    Objects.requireNonNull(expiresAt, "expiresAt");
    if (role == Role.OWNER) {
      throw new IllegalArgumentException("Invitations cannot grant ownership");
    }
  }

  public static Invitation create(
      HouseholdId household,
      EmailAddress email,
      Role role,
      String tokenHash,
      UserId invitedBy,
      Instant now) {
    return new Invitation(
        UUID.randomUUID(),
        household,
        email,
        role,
        tokenHash,
        invitedBy,
        now.plus(TIME_TO_LIVE),
        null);
  }

  public boolean isPending(Instant now) {
    return acceptedAt == null && now.isBefore(expiresAt);
  }

  public Invitation accept(Instant at) {
    if (acceptedAt != null) {
      throw new IllegalStateException("Invitation already accepted");
    }
    return new Invitation(id, household, email, role, tokenHash, invitedBy, expiresAt, at);
  }
}
