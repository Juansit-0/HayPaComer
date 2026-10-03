package dev.haypacomer.application.auth;

import dev.haypacomer.application.port.PasswordHasher;
import dev.haypacomer.application.port.RefreshTokenStore;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.application.port.UserTokenStore;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserToken;
import dev.haypacomer.domain.identity.UserTokenType;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class ResetPassword {

  private final UserRepository users;
  private final UserTokens tokens;
  private final PasswordHasher hasher;
  private final RefreshTokenStore refreshTokens;
  private final Clock clock;
  private final AuthSettings settings;

  public ResetPassword(
      UserRepository users,
      UserTokenStore tokenStore,
      OpaqueTokens opaqueTokens,
      PasswordHasher hasher,
      RefreshTokenStore refreshTokens,
      Clock clock,
      AuthSettings settings) {
    this.users = Objects.requireNonNull(users, "users");
    this.tokens = new UserTokens(tokenStore, opaqueTokens);
    this.hasher = Objects.requireNonNull(hasher, "hasher");
    this.refreshTokens = Objects.requireNonNull(refreshTokens, "refreshTokens");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  public void reset(String rawToken, String newPassword) {
    PasswordPolicy.requireAcceptable(newPassword, settings);
    Instant now = clock.instant();
    UserToken token = tokens.consume(rawToken, UserTokenType.RESET_PASSWORD, now);
    User user = users.findById(token.user()).orElseThrow(InvalidTokenException::new);
    users.save(user.changePassword(hasher.hash(newPassword)));
    refreshTokens.revokeAll(user.id(), now);
  }
}
