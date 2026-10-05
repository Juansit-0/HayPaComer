package dev.haypacomer.sensors.esp32;

import dev.haypacomer.application.port.ScaleCalibrationRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.WeightReading;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class Hx711ReadingAdapter {

  private final ScaleCalibrationRepository calibrations;
  private final ScaleSampleStore samples;

  public Hx711ReadingAdapter(ScaleCalibrationRepository calibrations, ScaleSampleStore samples) {
    this.calibrations = Objects.requireNonNull(calibrations, "calibrations");
    this.samples = Objects.requireNonNull(samples, "samples");
  }

  public Optional<WeightReading> adapt(
      Device device,
      SensorEventId id,
      long counts,
      Instant at,
      ScaleMode mode,
      Boolean reportedStable,
      String ingredient) {
    samples.record(device.id(), new RawSample(counts, at));
    return calibrations
        .find(device.id())
        .flatMap(calibration -> calibration.toGrams(counts))
        .map(
            grams ->
                new WeightReading(
                    id,
                    device.id(),
                    device.fridge(),
                    at,
                    grams,
                    stable(device, grams, at, reportedStable),
                    mode,
                    ingredient));
  }

  private boolean stable(Device device, Grams grams, Instant at, Boolean reportedStable) {
    boolean detected = samples.stable(device.id(), grams, at);
    return reportedStable == null ? detected : reportedStable;
  }
}
