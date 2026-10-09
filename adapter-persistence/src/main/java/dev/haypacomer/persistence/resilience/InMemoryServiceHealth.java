package dev.haypacomer.persistence.resilience;

import dev.haypacomer.application.port.ServiceHealth;
import dev.haypacomer.application.resilience.DegradedComponent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryServiceHealth implements ServiceHealth {

  private final Map<String, DegradedComponent> degraded = new ConcurrentHashMap<>();

  @Override
  public void degraded(String component, String reason, Instant at) {
    degraded.merge(
        component,
        new DegradedComponent(component, reason, at),
        (known, latest) -> new DegradedComponent(component, reason, known.since()));
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
