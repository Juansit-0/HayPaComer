package dev.haypacomer.application.notification;

import dev.haypacomer.application.port.NotificationPreferenceRepository;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public final class ViewNotificationPreferences {

  private final NotificationPreferenceRepository preferences;

  public ViewNotificationPreferences(NotificationPreferenceRepository preferences) {
    this.preferences = Objects.requireNonNull(preferences, "preferences");
  }

  public NotificationPreferences view(UserId actor) {
    return preferences.find(actor);
  }
}
