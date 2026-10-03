package dev.haypacomer.application.auth;

import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.application.mail.MailLinks;
import dev.haypacomer.application.port.EmailSender;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.application.port.UserTokenStore;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserTokenType;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class RequestPasswordReset {

  private final UserRepository users;
  private final UserTokens tokens;
  private final EmailSender email;
  private final MailLinks links;
  private final Clock clock;

  public RequestPasswordReset(
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

  public void request(String emailAddress) {
    Optional<User> user;
    try {
      user = users.findByEmail(new EmailAddress(emailAddress)).filter(User::canSignIn);
    } catch (IllegalArgumentException | NullPointerException exception) {
      return;
    }
    user.ifPresent(
        found -> {
          String raw = tokens.issue(found.id(), UserTokenType.RESET_PASSWORD, clock.instant());
          String link = links.to("reset-password", raw);
          email.send(
              new EmailMessage(
                  found.email(),
                  "Reset your HayPaComer password",
                  "Use this link within one hour to choose a new password: " + link,
                  link));
        });
  }
}
