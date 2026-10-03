package dev.haypacomer.application.auth;

import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.application.mail.MailLinks;
import dev.haypacomer.application.port.AccessTokenIssuer;
import dev.haypacomer.application.port.EmailSender;
import dev.haypacomer.application.port.LoginAttemptLog;
import dev.haypacomer.application.port.PasswordHasher;
import dev.haypacomer.application.port.RefreshTokenStore;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.application.port.UserTokenStore;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.PasswordHash;
import dev.haypacomer.domain.identity.RefreshToken;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.identity.UserToken;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class AuthFakes {

  final MutableClock clock = new MutableClock(Instant.parse("2026-10-03T12:00:00Z"));
  final Users users = new Users();
  final Hasher hasher = new Hasher();
  final Attempts attempts = new Attempts();
  final Tokens refreshTokens = new Tokens();
  final OpaqueTokens opaqueTokens = new OpaqueTokens();
  final AccessTokenIssuer accessTokens =
      (user, now) -> new AccessToken("access-" + user.value(), now.plus(Duration.ofMinutes(15)));
  final TokenStore userTokens = new TokenStore();
  final Outbox outbox = new Outbox();
  final MailLinks links = new MailLinks("https://haypacomer.dev/");
  final SessionIssuer sessions =
      new SessionIssuer(accessTokens, refreshTokens, opaqueTokens, AuthSettings.DEFAULT);

  RegisterUser registerUser() {
    return new RegisterUser(users, hasher, clock, AuthSettings.DEFAULT);
  }

  LogIn logIn() {
    return new LogIn(users, hasher, attempts, sessions, clock, AuthSettings.DEFAULT);
  }

  RefreshSession refreshSession() {
    return new RefreshSession(refreshTokens, users, sessions, opaqueTokens, clock);
  }

  LogOut logOut() {
    return new LogOut(refreshTokens, opaqueTokens, clock);
  }

  RequestEmailVerification requestEmailVerification() {
    return new RequestEmailVerification(users, userTokens, opaqueTokens, outbox, links, clock);
  }

  VerifyEmail verifyEmail() {
    return new VerifyEmail(users, userTokens, opaqueTokens, clock);
  }

  RequestPasswordReset requestPasswordReset() {
    return new RequestPasswordReset(users, userTokens, opaqueTokens, outbox, links, clock);
  }

  ResetPassword resetPassword() {
    return new ResetPassword(
        users, userTokens, opaqueTokens, hasher, refreshTokens, clock, AuthSettings.DEFAULT);
  }

  static final class TokenStore implements UserTokenStore {

    final Map<UUID, UserToken> byId = new HashMap<>();

    @Override
    public void save(UserToken token) {
      byId.put(token.id(), token);
    }

    @Override
    public Optional<UserToken> findByHash(String tokenHash) {
      return byId.values().stream()
          .filter(token -> token.tokenHash().equals(tokenHash))
          .findFirst();
    }
  }

  static final class Outbox implements EmailSender {

    final List<EmailMessage> sent = new ArrayList<>();

    @Override
    public void send(EmailMessage message) {
      sent.add(message);
    }

    String lastToken() {
      String link = sent.getLast().link();
      return URLDecoder.decode(link.substring(link.indexOf("#token=") + 7), StandardCharsets.UTF_8);
    }
  }

  static final class MutableClock extends Clock {

    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  static final class Users implements UserRepository {

    final Map<UserId, User> byId = new HashMap<>();

    @Override
    public void save(User user) {
      byId.put(user.id(), user);
    }

    @Override
    public Optional<User> findById(UserId id) {
      return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
      return byId.values().stream().filter(user -> user.email().equals(email)).findFirst();
    }
  }

  static final class Hasher implements PasswordHasher {

    int hashes;

    @Override
    public PasswordHash hash(String rawPassword) {
      hashes++;
      return new PasswordHash("hashed:" + rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
      return hash.value().equals("hashed:" + rawPassword);
    }
  }

  static final class Attempts implements LoginAttemptLog {

    final List<Attempt> log = new ArrayList<>();

    @Override
    public void record(EmailAddress email, boolean success, Instant at) {
      log.add(new Attempt(email, success, at));
    }

    @Override
    public int failuresSince(EmailAddress email, Instant since) {
      return (int)
          log.stream()
              .filter(attempt -> attempt.email().equals(email))
              .filter(attempt -> !attempt.success())
              .filter(attempt -> !attempt.at().isBefore(since))
              .count();
    }

    record Attempt(EmailAddress email, boolean success, Instant at) {}
  }

  static final class Tokens implements RefreshTokenStore {

    final Map<UUID, RefreshToken> byId = new HashMap<>();

    @Override
    public void save(RefreshToken token) {
      byId.put(token.id(), token);
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
      return byId.values().stream()
          .filter(token -> token.tokenHash().equals(tokenHash))
          .findFirst();
    }

    @Override
    public void revokeFamily(UUID family, Instant at) {
      byId.replaceAll((id, token) -> token.family().equals(family) ? token.revoke(at) : token);
    }

    @Override
    public void revokeAll(UserId user, Instant at) {
      byId.replaceAll((id, token) -> token.user().equals(user) ? token.revoke(at) : token);
    }

    List<RefreshToken> family(UUID family) {
      return byId.values().stream().filter(token -> token.family().equals(family)).toList();
    }
  }
}
