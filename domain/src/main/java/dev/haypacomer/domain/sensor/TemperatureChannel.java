package dev.haypacomer.domain.sensor;

import java.util.Optional;

public final class TemperatureChannel extends MeasurementChannel {

  @Override
  public String name() {
    return "temperature";
  }

  @Override
  protected Optional<Measurement> measure(SensorEvent event) {
    if (event instanceof TemperatureReading reading) {
      return Optional.of(new Measurement(reading.occurredAt(), reading.celsius(), true));
    }
    return Optional.empty();
  }
}
