package dev.haypacomer.application.port;

import dev.haypacomer.application.notification.InboxEntry;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationInbox {

  List<InboxEntry> recent(UserId user, int limit);

  boolean markRead(UserId user, UUID notification, Instant at);
}
