package dev.haypacomer.sensors.esp32;

import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import java.util.Optional;

public abstract class Esp32EventFactory {

  public Optional<SensorEvent> create(Esp32Envelope envelope, Device device) {
    if (envelope.eventId() == null) {
      throw new MalformedSensorPayloadException("eventId is required");
    }
    if (envelope.at() == null) {
      throw new MalformedSensorPayloadException("at is required");
    }
    return Optional.of(build(envelope, device, new SensorEventId(envelope.eventId())));
  }

  protected abstract SensorEvent build(Esp32Envelope envelope, Device device, SensorEventId id);

  protected static <T> T required(T value, String field) {
    if (value == null) {
      throw new MalformedSensorPayloadException(field + " is required");
    }
    return value;
  }
}
