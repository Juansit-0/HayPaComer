package dev.haypacomer.domain.sensor;

import java.util.Locale;
import java.util.Optional;

public final class WeightChannel extends MeasurementChannel {

  private final ScaleMode mode;

  public WeightChannel(ScaleMode mode) {
    this.mode = mode;
  }

  @Override
  public String name() {
    return "weight-" + mode.name().toLowerCase(Locale.ROOT);
  }

  @Override
  protected Optional<Measurement> measure(SensorEvent event) {
    if (event instanceof WeightReading reading && reading.mode() == mode) {
      return Optional.of(
          new Measurement(reading.occurredAt(), reading.grams().value(), reading.stable()));
    }
    return Optional.empty();
  }
}
