package dev.haypacomer.domain.sensor;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.quantity.Grams;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record WeightReading(
    SensorEventId id,
    DeviceId device,
    FridgeId fridge,
    Instant occurredAt,
    Grams grams,
    boolean stable,
    ScaleMode mode,
    String ingredient)
    implements SensorEvent {

  public WeightReading {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(device, "device");
    Objects.requireNonNull(fridge, "fridge");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(grams, "grams");
    Objects.requireNonNull(mode, "mode");
    ingredient = ingredient == null || ingredient.isBlank() ? null : ingredient.strip();
  }

  public Optional<String> ingredientHint() {
    return Optional.ofNullable(ingredient);
  }
}
