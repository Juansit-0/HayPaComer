package dev.haypacomer.sensors.esp32;

import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.WeightReading;
import java.util.Locale;

public final class WeightEventFactory extends Esp32EventFactory {

  @Override
  protected SensorEvent build(Esp32Envelope envelope, Device device, SensorEventId id) {
    String mode =
        envelope.mode() == null ? "FRIDGE" : envelope.mode().strip().toUpperCase(Locale.ROOT);
    ScaleMode scaleMode =
        switch (mode) {
          case "FRIDGE" -> ScaleMode.FRIDGE;
          case "COOK", "COOKING" -> ScaleMode.COOKING;
          default -> throw new MalformedSensorPayloadException("mode must be FRIDGE or COOK");
        };
    if (required(envelope.grams(), "grams").signum() < 0) {
      throw new MalformedSensorPayloadException("grams cannot be negative");
    }
    return new WeightReading(
        id,
        device.id(),
        device.fridge(),
        envelope.at(),
        Grams.of(envelope.grams()),
        required(envelope.stable(), "stable"),
        scaleMode,
        envelope.ingredient());
  }
}
