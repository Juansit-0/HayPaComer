package dev.haypacomer.application.port;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.sensor.SensorEvent;
import java.time.Instant;
import java.util.List;

public interface SensorHistory {

  List<SensorEvent> doorAndTemperature(FridgeId fridge, Instant from, Instant to);
}
