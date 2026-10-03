package dev.haypacomer.domain.sensor;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import java.time.Instant;

public sealed interface SensorEvent permits DoorEvent, TemperatureReading, WeightReading {

  SensorEventId id();

  DeviceId device();

  FridgeId fridge();

  Instant occurredAt();
}
