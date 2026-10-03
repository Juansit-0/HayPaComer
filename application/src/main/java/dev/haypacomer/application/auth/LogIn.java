package dev.haypacomer.application.auth;

import dev.haypacomer.application.port.LoginAttemptLog;
import dev.haypacomer.application.port.PasswordHasher;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class LogIn {

  private final UserRepository users;
  private final PasswordHasher hasher;
  private final LoginAttemptLog attempts;
  private final SessionIssuer sessions;
  private final Clock clock;
  private final AuthSettings settings;

  public LogIn(
      UserRepository users,
      PasswordHasher hasher,
      LoginAttemptLog attempts,
      SessionIssuer sessions,
      Clock clock,
      AuthSettings settings) {
    this.users = Objects.requireNonNull(users, "users");
    this.hasher = Objects.requireNonNull(hasher, "hasher");
    this.attempts = Objects.requireNonNull(attempts, "attempts");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  public AuthTokens logIn(LoginCommand command) {
    Instant now = clock.instant();
    EmailAddress email = parse(command.email());
    if (attempts.failuresSince(email, now.minus(settings.lockoutWindow()))
        >= settings.maxFailedLogins()) {
      throw new TooManyLoginAttemptsException();
    }
    String password = command.password() == null ? "" : command.password();
    Optional<User> user = users.findByEmail(email);
    boolean valid = verify(user, password);
    attempts.record(email, valid, now);
    if (!valid) {
      throw new InvalidCredentialsException();
    }
    return sessions.issue(user.orElseThrow().id(), UUID.randomUUID(), now).tokens();
  }

  private boolean verify(Optional<User> user, String password) {
    if (user.isEmpty()) {
      hasher.hash(password);
      return false;
    }
    return user.get().canSignIn() && hasher.matches(password, user.get().passwordHash());
  }

  private static EmailAddress parse(String email) {
    try {
      return new EmailAddress(email);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new InvalidCredentialsException();
    }
  }
}
