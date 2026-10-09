package dev.haypacomer.application.resilience;

import java.util.List;

public record ServiceStatus(List<DegradedComponent> degraded) {

  public ServiceStatus {
    degraded = List.copyOf(degraded);
  }

  public boolean healthy() {
    return degraded.isEmpty();
  }
}
