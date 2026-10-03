package dev.haypacomer.domain.identity;

import java.time.Instant;
import java.util.Objects;

public record User(
    UserId id,
    EmailAddress email,
    PasswordHash passwordHash,
    String displayName,
    boolean emailVerified,
    boolean enabled,
    Instant createdAt) {

  public User {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(passwordHash, "passwordHash");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(createdAt, "createdAt");
    displayName = displayName.strip();
    if (displayName.isEmpty()) {
      throw new IllegalArgumentException("Display name cannot be blank");
    }
  }

  public static User register(
      EmailAddress email, PasswordHash passwordHash, String displayName, Instant now) {
    return new User(UserId.newId(), email, passwordHash, displayName, false, true, now);
  }

  public User verifyEmail() {
    return new User(id, email, passwordHash, displayName, true, enabled, createdAt);
  }

  public User changePassword(PasswordHash newHash) {
    return new User(id, email, newHash, displayName, emailVerified, enabled, createdAt);
  }

  public User rename(String newDisplayName) {
    return new User(id, email, passwordHash, newDisplayName, emailVerified, enabled, createdAt);
  }

  public User disable() {
    return new User(id, email, passwordHash, displayName, emailVerified, false, createdAt);
  }

  public boolean canSignIn() {
    return enabled;
  }
}
