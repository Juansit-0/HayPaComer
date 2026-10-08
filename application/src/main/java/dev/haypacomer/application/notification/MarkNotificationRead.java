package dev.haypacomer.application.notification;

import dev.haypacomer.application.port.NotificationInbox;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class MarkNotificationRead {

  private final NotificationInbox inbox;
  private final Clock clock;

  public MarkNotificationRead(NotificationInbox inbox, Clock clock) {
    this.inbox = Objects.requireNonNull(inbox, "inbox");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public void mark(UserId actor, UUID notification) {
    if (!inbox.markRead(actor, notification, clock.instant())) {
      throw new NotificationNotFoundException();
    }
  }
}
