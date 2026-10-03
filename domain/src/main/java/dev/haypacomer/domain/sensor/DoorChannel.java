package dev.haypacomer.domain.sensor;

import java.math.BigDecimal;
import java.util.Optional;

public final class DoorChannel extends MeasurementChannel {

  @Override
  public String name() {
    return "door";
  }

  @Override
  protected Optional<Measurement> measure(SensorEvent event) {
    if (event instanceof DoorEvent door) {
      BigDecimal value = door.state() == DoorState.OPEN ? BigDecimal.ONE : BigDecimal.ZERO;
      return Optional.of(new Measurement(door.occurredAt(), value, true));
    }
    return Optional.empty();
  }
}
