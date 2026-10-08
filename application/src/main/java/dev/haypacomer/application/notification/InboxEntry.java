package dev.haypacomer.application.notification;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record InboxEntry(Notification notification, Instant readAt) {

  public InboxEntry {
    Objects.requireNonNull(notification, "notification");
  }

  public Optional<Instant> read() {
    return Optional.ofNullable(readAt);
  }
}
