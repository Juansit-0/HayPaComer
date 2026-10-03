package dev.haypacomer.sensors.esp32;

import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import java.util.Locale;

public final class DoorEventFactory extends Esp32EventFactory {

  @Override
  protected SensorEvent build(Esp32Envelope envelope, Device device, SensorEventId id) {
    String door = required(envelope.door(), "door").strip().toUpperCase(Locale.ROOT);
    DoorState state =
        switch (door) {
          case "OPEN" -> DoorState.OPEN;
          case "CLOSED" -> DoorState.CLOSED;
          default -> throw new MalformedSensorPayloadException("door must be OPEN or CLOSED");
        };
    return new DoorEvent(id, device.id(), device.fridge(), envelope.at(), state);
  }
}
