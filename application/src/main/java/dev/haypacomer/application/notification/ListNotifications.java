package dev.haypacomer.application.notification;

import dev.haypacomer.application.port.NotificationInbox;
import dev.haypacomer.domain.identity.UserId;
import java.util.List;
import java.util.Objects;

public final class ListNotifications {

  public static final int LIMIT = 50;

  private final NotificationInbox inbox;

  public ListNotifications(NotificationInbox inbox) {
    this.inbox = Objects.requireNonNull(inbox, "inbox");
  }

  public List<InboxEntry> list(UserId actor) {
    return inbox.recent(actor, LIMIT);
  }
}
