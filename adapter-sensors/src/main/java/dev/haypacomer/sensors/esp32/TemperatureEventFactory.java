package dev.haypacomer.sensors.esp32;

import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;

public final class TemperatureEventFactory extends Esp32EventFactory {

  @Override
  protected SensorEvent build(Esp32Envelope envelope, Device device, SensorEventId id) {
    return new TemperatureReading(
        id, device.id(), device.fridge(), envelope.at(), required(envelope.tempC(), "tempC"));
  }
}
