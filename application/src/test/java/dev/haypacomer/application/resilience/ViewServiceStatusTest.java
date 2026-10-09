package dev.haypacomer.application.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.port.ServiceHealth;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViewServiceStatusTest {

  @Test
  void listsDegradedComponentsByName() {
    Instant now = Instant.parse("2026-10-09T18:00:00Z");
    List<DegradedComponent> current =
        List.of(
            new DegradedComponent("prices", "Serving the last saved copy", now),
            new DegradedComponent("catalog", "Serving the last saved copy", now));
    ServiceHealth health =
        new ServiceHealth() {
          @Override
          public void degraded(String component, String reason, Instant at) {}

          @Override
          public void recovered(String component) {}

          @Override
          public List<DegradedComponent> current() {
            return current;
          }
        };

    ServiceStatus status = new ViewServiceStatus(health).view();

    assertFalse(status.healthy());
    assertEquals("catalog", status.degraded().getFirst().component());
    assertTrue(new ServiceStatus(List.of()).healthy());
  }
}
