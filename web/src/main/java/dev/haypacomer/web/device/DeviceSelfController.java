package dev.haypacomer.web.device;

import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.application.sensor.EventResult;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.application.sensor.IngestSensorEvents;
import dev.haypacomer.application.sensor.IngestionReport;
import dev.haypacomer.application.sensor.validation.Verdict;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FindingKind;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceSelfController {

  private final HardwareFactories hardware;
  private final IngestSensorEvents ingestSensorEvents;

  public DeviceSelfController(HardwareFactories hardware, IngestSensorEvents ingestSensorEvents) {
    this.hardware = hardware;
    this.ingestSensorEvents = ingestSensorEvents;
  }

  @PostMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<IngestionResponse> events(
      @AuthenticationPrincipal Device device, @RequestBody String payload) {
    IngestionReport report = ingestSensorEvents.ingest(device, payload);
    HttpStatus status =
        report.allRejected()
            ? HttpStatus.BAD_REQUEST
            : report.allDuplicates() ? HttpStatus.OK : HttpStatus.ACCEPTED;
    return ResponseEntity.status(status).body(IngestionResponse.from(report));
  }

  @GetMapping("/commands")
  List<AlertPattern> commands(@AuthenticationPrincipal Device device) {
    return hardware.forDevice(device).alerts().drain(device.id());
  }

  @GetMapping("/whoami")
  Identity whoami(@AuthenticationPrincipal Device device) {
    return new Identity(
        device.id().value(),
        device.household().value(),
        device.fridge().value(),
        device.name(),
        device.kind());
  }

  record IngestionResponse(
      long accepted,
      long duplicates,
      long dropped,
      long rejected,
      List<EventResult> events,
      List<FindingKind> findings) {

    static IngestionResponse from(IngestionReport report) {
      return new IngestionResponse(
          report.count(Verdict.ACCEPTED),
          report.count(Verdict.DUPLICATE),
          report.count(Verdict.DROPPED),
          report.count(Verdict.REJECTED),
          report.events(),
          report.findings().stream().map(Finding::kind).toList());
    }
  }

  record Identity(UUID id, UUID householdId, UUID fridgeId, String name, DeviceKind kind) {}
}
