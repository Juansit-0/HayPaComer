package dev.haypacomer.application.sensor.validation;

import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.TemperatureReading;
import dev.haypacomer.domain.sensor.WeightReading;

public final class SensorEventTypes {

  private SensorEventTypes() {}

  public static String of(SensorEvent event) {
    return switch (event) {
      case DoorEvent door -> "DOOR";
      case TemperatureReading reading -> "TEMPERATURE";
      case WeightReading reading -> "WEIGHT";
    };
  }
}
