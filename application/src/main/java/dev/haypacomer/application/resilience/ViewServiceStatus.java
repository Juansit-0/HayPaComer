package dev.haypacomer.application.resilience;

import dev.haypacomer.application.port.ServiceHealth;
import java.util.Comparator;
import java.util.Objects;

public final class ViewServiceStatus {

  private final ServiceHealth health;

  public ViewServiceStatus(ServiceHealth health) {
    this.health = Objects.requireNonNull(health, "health");
  }

  public ServiceStatus view() {
    return new ServiceStatus(
        health.current().stream()
            .sorted(Comparator.comparing(DegradedComponent::component))
            .toList());
  }
}
