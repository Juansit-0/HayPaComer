package dev.haypacomer.application.live;

import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.port.NotificationListener;
import java.util.Objects;

public final class AlertsToLive implements NotificationListener {

  private final BroadcastLiveUpdate live;

  public AlertsToLive(BroadcastLiveUpdate live) {
    this.live = Objects.requireNonNull(live, "live");
  }

  @Override
  public void onNotification(Notification notification) {
    live.publish(
        LiveUpdate.of(
            notification.household(),
            LiveUpdateKind.ALERT,
            null,
            notification.title() + ". " + notification.body(),
            notification.at()));
  }
}
