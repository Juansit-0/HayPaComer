package dev.haypacomer.notifications;

import dev.haypacomer.application.notification.ChannelKind;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationPreferences;
import dev.haypacomer.application.port.NotificationChannel;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public final class LogNotificationChannel implements NotificationChannel {

  private static final Logger LOG = System.getLogger(LogNotificationChannel.class.getName());

  @Override
  public ChannelKind kind() {
    return ChannelKind.LOG;
  }

  @Override
  public void deliver(Notification notification, NotificationPreferences recipient) {
    LOG.log(
        Level.INFO,
        "Notification {0} for user {1}: {2}",
        notification.type(),
        recipient.user().value(),
        notification.title());
  }
}
