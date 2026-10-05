package dev.haypacomer.application.sensor;

import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.WeightReading;
import java.util.Optional;

@FunctionalInterface
public interface WeightReadingHandler {

  Optional<CommandOutcome> apply(Device scale, WeightReading reading);
}
