package dev.haypacomer.application.port;

import dev.haypacomer.domain.identity.EmailAddress;
import java.time.Instant;

public interface LoginAttemptLog {

  void record(EmailAddress email, boolean success, Instant at);

  int failuresSince(EmailAddress email, Instant since);
}
