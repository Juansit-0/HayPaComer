package dev.haypacomer.notifications;

import dev.haypacomer.application.mail.EmailMessage;
import dev.haypacomer.application.port.EmailSender;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class LogEmailSender implements EmailSender {

  private static final Logger LOG = System.getLogger(LogEmailSender.class.getName());

  private final boolean revealLinks;

  public LogEmailSender(boolean revealLinks) {
    this.revealLinks = revealLinks;
  }

  @Override
  public void send(EmailMessage message) {
    if (revealLinks) {
      LOG.log(
          Level.INFO, "Email to {0}: {1} | {2}", message.to(), message.subject(), message.link());
    } else {
      LOG.log(Level.INFO, "Email to {0}: {1}", message.to(), message.subject());
    }
  }
}
