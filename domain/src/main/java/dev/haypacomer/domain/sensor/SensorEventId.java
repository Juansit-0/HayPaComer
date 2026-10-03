package dev.haypacomer.domain.sensor;

import java.util.Objects;
import java.util.UUID;

public record SensorEventId(UUID value) {

  public SensorEventId {
    Objects.requireNonNull(value, "value");
  }
}
