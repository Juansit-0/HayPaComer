package dev.haypacomer.sensors.esp32;

import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import java.util.Locale;
import java.util.Optional;

public final class RawWeightEventFactory extends Esp32EventFactory {

  private final Hx711ReadingAdapter hx711;

  public RawWeightEventFactory(Hx711ReadingAdapter hx711) {
    this.hx711 = hx711;
  }

  @Override
  public Optional<SensorEvent> create(Esp32Envelope envelope, Device device) {
    if (envelope.eventId() == null || envelope.at() == null) {
      throw new MalformedSensorPayloadException("eventId and at are required");
    }
    ScaleMode scaleMode =
        envelope.mode() == null
            ? null
            : switch (envelope.mode().strip().toUpperCase(Locale.ROOT)) {
              case "FRIDGE" -> ScaleMode.FRIDGE;
              case "COOK", "COOKING" -> ScaleMode.COOKING;
              default -> throw new MalformedSensorPayloadException("mode must be FRIDGE or COOK");
            };
    return hx711
        .adapt(
            device,
            new SensorEventId(envelope.eventId()),
            required(envelope.raw(), "raw"),
            envelope.at(),
            scaleMode,
            envelope.stable(),
            envelope.ingredient())
        .map(SensorEvent.class::cast);
  }

  @Override
  protected SensorEvent build(Esp32Envelope envelope, Device device, SensorEventId id) {
    throw new UnsupportedOperationException("Raw weights are adapted through the HX711 adapter");
  }
}
