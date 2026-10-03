package dev.haypacomer.application.auth;

import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.application.port.UserTokenStore;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserToken;
import dev.haypacomer.domain.identity.UserTokenType;
import java.time.Clock;
import java.util.Objects;

public final class VerifyEmail {

  private final UserRepository users;
  private final UserTokens tokens;
  private final Clock clock;

  public VerifyEmail(
      UserRepository users, UserTokenStore tokenStore, OpaqueTokens opaqueTokens, Clock clock) {
    this.users = Objects.requireNonNull(users, "users");
    this.tokens = new UserTokens(tokenStore, opaqueTokens);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public User verify(String rawToken) {
    UserToken token = tokens.consume(rawToken, UserTokenType.VERIFY_EMAIL, clock.instant());
    User verified =
        users.findById(token.user()).orElseThrow(InvalidTokenException::new).verifyEmail();
    users.save(verified);
    return verified;
  }
}
