package dev.haypacomer.sensors.hardware;

import dev.haypacomer.application.port.AlertSignal;
import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.domain.device.DeviceId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QueuedAlertSignal implements AlertSignal {

  private static final int MAX_PENDING = 20;

  private final Map<DeviceId, Deque<AlertPattern>> pending = new ConcurrentHashMap<>();

  @Override
  public void signal(DeviceId device, AlertPattern pattern) {
    Deque<AlertPattern> queue = pending.computeIfAbsent(device, key -> new ArrayDeque<>());
    synchronized (queue) {
      if (queue.size() == MAX_PENDING) {
        queue.removeFirst();
      }
      queue.addLast(pattern);
    }
  }

  @Override
  public List<AlertPattern> drain(DeviceId device) {
    Deque<AlertPattern> queue = pending.get(device);
    if (queue == null) {
      return List.of();
    }
    synchronized (queue) {
      List<AlertPattern> drained = new ArrayList<>(queue);
      queue.clear();
      return List.copyOf(drained);
    }
  }
}
