package dev.haypacomer.application.auth;

import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.application.mail.MailLinks;
import dev.haypacomer.application.port.EmailSender;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.application.port.UserTokenStore;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.identity.UserTokenType;
import java.time.Clock;
import java.util.Objects;

public final class RequestEmailVerification {

  private final UserRepository users;
  private final UserTokens tokens;
  private final EmailSender email;
  private final MailLinks links;
  private final Clock clock;

  public RequestEmailVerification(
      UserRepository users,
      UserTokenStore tokenStore,
      OpaqueTokens opaqueTokens,
      EmailSender email,
      MailLinks links,
      Clock clock) {
    this.users = Objects.requireNonNull(users, "users");
    this.tokens = new UserTokens(tokenStore, opaqueTokens);
    this.email = Objects.requireNonNull(email, "email");
    this.links = Objects.requireNonNull(links, "links");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public void request(UserId id) {
    User user = users.findById(id).orElseThrow(UserNotFoundException::new);
    if (user.emailVerified()) {
      return;
    }
    String raw = tokens.issue(user.id(), UserTokenType.VERIFY_EMAIL, clock.instant());
    String link = links.to("verify-email", raw);
    email.send(
        new EmailMessage(
            user.email(),
            "Confirm your email for HayPaComer",
            "Hi " + user.displayName() + ", confirm your email within 24 hours: " + link,
            link));
  }
}
