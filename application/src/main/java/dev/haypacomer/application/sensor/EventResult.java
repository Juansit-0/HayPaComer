package dev.haypacomer.application.sensor;

import dev.haypacomer.application.sensor.validation.Verdict;
import java.util.Objects;
import java.util.UUID;

public record EventResult(UUID eventId, String type, Verdict verdict, String reason) {

  public EventResult {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(verdict, "verdict");
    Objects.requireNonNull(reason, "reason");
  }
}
