package dev.haypacomer.application.port;

import dev.haypacomer.application.notification.ChannelKind;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationPreferences;

public interface NotificationChannel {

  ChannelKind kind();

  void deliver(Notification notification, NotificationPreferences recipient);
}
