package dev.haypacomer.application.port;

import dev.haypacomer.application.notification.NotificationPreferences;
import dev.haypacomer.domain.identity.UserId;

public interface NotificationPreferenceRepository {

  NotificationPreferences find(UserId user);

  void save(NotificationPreferences preferences);
}
