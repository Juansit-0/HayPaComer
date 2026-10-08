package dev.haypacomer.application.notification;

import dev.haypacomer.application.port.NotificationListener;
import java.util.List;
import java.util.Objects;

public final class NotifyHousehold {

  public static final NotifyHousehold NOBODY = new NotifyHousehold(List.of());

  private final List<NotificationListener> listeners;

  public NotifyHousehold(List<NotificationListener> listeners) {
    this.listeners = List.copyOf(Objects.requireNonNull(listeners, "listeners"));
  }

  public void publish(Notification notification) {
    Objects.requireNonNull(notification, "notification");
    listeners.forEach(listener -> listener.onNotification(notification));
  }
}
