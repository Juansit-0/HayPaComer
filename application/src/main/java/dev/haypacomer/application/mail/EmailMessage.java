package dev.haypacomer.application.mail;

import dev.haypacomer.domain.identity.EmailAddress;
import java.util.Objects;

public record EmailMessage(EmailAddress to, String subject, String body, String link) {

  public EmailMessage {
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(link, "link");
  }

  @Override
  public String toString() {
    return "EmailMessage[to=" + to + ", subject=" + subject + "]";
  }
}
