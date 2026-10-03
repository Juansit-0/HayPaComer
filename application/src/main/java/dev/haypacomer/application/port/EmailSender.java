package dev.haypacomer.application.port;

import dev.haypacomer.application.mail.EmailMessage;

public interface EmailSender {

  void send(EmailMessage message);
}
