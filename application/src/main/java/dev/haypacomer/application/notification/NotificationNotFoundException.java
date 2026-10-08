package dev.haypacomer.application.notification;

public final class NotificationNotFoundException extends RuntimeException {

  public NotificationNotFoundException() {
    super("Notification not found");
  }
}
