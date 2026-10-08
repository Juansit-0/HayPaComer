package dev.haypacomer.application.live;

import dev.haypacomer.application.port.LiveUpdateListener;
import java.util.List;
import java.util.Objects;

public final class BroadcastLiveUpdate {

  public static final BroadcastLiveUpdate NOBODY = new BroadcastLiveUpdate(List.of());

  private final List<LiveUpdateListener> listeners;

  public BroadcastLiveUpdate(List<LiveUpdateListener> listeners) {
    this.listeners = List.copyOf(Objects.requireNonNull(listeners, "listeners"));
  }

  public void publish(LiveUpdate update) {
    Objects.requireNonNull(update, "update");
    listeners.forEach(listener -> listener.onUpdate(update));
  }
}
