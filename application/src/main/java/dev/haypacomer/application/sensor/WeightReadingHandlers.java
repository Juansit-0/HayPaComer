package dev.haypacomer.application.sensor;

import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.WeightReading;
import java.util.List;
import java.util.Optional;

public final class WeightReadingHandlers implements WeightReadingHandler {

  private final List<WeightReadingHandler> handlers;

  public WeightReadingHandlers(List<WeightReadingHandler> handlers) {
    this.handlers = List.copyOf(handlers);
  }

  @Override
  public Optional<CommandOutcome> apply(Device scale, WeightReading reading) {
    Optional<CommandOutcome> outcome = Optional.empty();
    for (WeightReadingHandler handler : handlers) {
      Optional<CommandOutcome> handled = handler.apply(scale, reading);
      if (outcome.isEmpty()) {
        outcome = handled;
      }
    }
    return outcome;
  }
}
