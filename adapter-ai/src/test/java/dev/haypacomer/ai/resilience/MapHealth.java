package dev.haypacomer.ai.resilience;

import dev.haypacomer.application.port.ServiceHealth;
import dev.haypacomer.application.resilience.DegradedComponent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MapHealth implements ServiceHealth {

  final Map<String, DegradedComponent> degraded = new ConcurrentHashMap<>();

  @Override
  public void degraded(String component, String reason, Instant at) {
    degraded.putIfAbsent(component, new DegradedComponent(component, reason, at));
  }

  @Override
  public void recovered(String component) {
    degraded.remove(component);
  }

  @Override
  public List<DegradedComponent> current() {
    return List.copyOf(degraded.values());
  }
}
