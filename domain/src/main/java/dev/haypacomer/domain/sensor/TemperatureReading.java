package dev.haypacomer.domain.sensor;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record TemperatureReading(
    SensorEventId id, DeviceId device, FridgeId fridge, Instant occurredAt, BigDecimal celsius)
    implements SensorEvent {

  public TemperatureReading {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(device, "device");
    Objects.requireNonNull(fridge, "fridge");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(celsius, "celsius");
  }
}
