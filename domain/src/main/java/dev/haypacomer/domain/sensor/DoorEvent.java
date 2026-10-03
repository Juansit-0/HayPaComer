package dev.haypacomer.domain.sensor;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import java.time.Instant;
import java.util.Objects;

public record DoorEvent(
    SensorEventId id, DeviceId device, FridgeId fridge, Instant occurredAt, DoorState state)
    implements SensorEvent {

  public DoorEvent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(device, "device");
    Objects.requireNonNull(fridge, "fridge");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(state, "state");
  }
}
