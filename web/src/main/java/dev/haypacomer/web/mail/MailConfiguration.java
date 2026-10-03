package dev.haypacomer.web.mail;

import dev.haypacomer.application.mail.MailLinks;
import dev.haypacomer.application.port.EmailSender;
import dev.haypacomer.notifications.LogEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfiguration {

  @Bean
  MailLinks mailLinks(@Value("${haypacomer.mail.base-url}") String baseUrl) {
    return new MailLinks(baseUrl);
  }

  @Bean
  EmailSender emailSender(@Value("${haypacomer.mail.log-links:false}") boolean logLinks) {
    return new LogEmailSender(logLinks);
  }
}
