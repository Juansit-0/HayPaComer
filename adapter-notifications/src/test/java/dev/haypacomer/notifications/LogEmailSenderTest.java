package dev.haypacomer.notifications;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.domain.identity.EmailAddress;
import org.junit.jupiter.api.Test;

class LogEmailSenderTest {

  private final EmailMessage message =
      new EmailMessage(
          new EmailAddress("ana@haypacomer.dev"),
          "Join",
          "Body",
          "https://haypacomer.dev/join#token=x");

  @Test
  void logsWithOrWithoutLinks() {
    assertDoesNotThrow(() -> new LogEmailSender(false).send(message));
    assertDoesNotThrow(() -> new LogEmailSender(true).send(message));
  }
}
