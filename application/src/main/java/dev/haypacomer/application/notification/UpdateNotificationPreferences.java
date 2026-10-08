package dev.haypacomer.application.notification;

import dev.haypacomer.application.port.NotificationPreferenceRepository;
import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;
import java.util.Set;

public final class UpdateNotificationPreferences {

  private final NotificationPreferenceRepository preferences;

  public UpdateNotificationPreferences(NotificationPreferenceRepository preferences) {
    this.preferences = Objects.requireNonNull(preferences, "preferences");
  }

  public NotificationPreferences update(
      UserId actor, Set<ChannelKind> channels, String telegramChatId) {
    NotificationPreferences updated = new NotificationPreferences(actor, channels, telegramChatId);
    preferences.save(updated);
    return updated;
  }
}
