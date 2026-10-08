package dev.haypacomer.application.port;

import dev.haypacomer.application.notification.Notification;

public interface NotificationListener {

  void onNotification(Notification notification);
}
