package dev.haypacomer.web.resilience;

import dev.haypacomer.application.resilience.DegradedComponent;
import dev.haypacomer.application.resilience.ServiceStatus;
import dev.haypacomer.application.resilience.ViewServiceStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

  private final ViewServiceStatus viewStatus;

  public StatusController(ViewServiceStatus viewStatus) {
    this.viewStatus = viewStatus;
  }

  @GetMapping
  StatusResponse status() {
    ServiceStatus status = viewStatus.view();
    return new StatusResponse(
        status.healthy() ? "OK" : "DEGRADED",
        status.degraded().stream().map(ComponentResponse::from).toList());
  }

  record ComponentResponse(String component, String reason, Instant since) {

    static ComponentResponse from(DegradedComponent component) {
      return new ComponentResponse(component.component(), component.reason(), component.since());
    }
  }

  record StatusResponse(String state, List<ComponentResponse> degraded) {}
}
