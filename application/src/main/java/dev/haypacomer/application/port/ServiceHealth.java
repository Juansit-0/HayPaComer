package dev.haypacomer.application.port;

import dev.haypacomer.application.resilience.DegradedComponent;
import java.time.Instant;
import java.util.List;

public interface ServiceHealth {

  void degraded(String component, String reason, Instant at);

  void recovered(String component);

  List<DegradedComponent> current();
}
