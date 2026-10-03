package dev.haypacomer.domain.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SensorEventTest {

  private static final Instant NOW = Instant.parse("2026-10-03T18:40:00Z");
  private final SensorEventId id = new SensorEventId(UUID.randomUUID());
  private final DeviceId device = DeviceId.newId();
  private final FridgeId fridge = FridgeId.newId();

  @Test
  void weightKeepsAnOptionalIngredientHint() {
    WeightReading named =
        new WeightReading(
            id, device, fridge, NOW, Grams.of(132), true, ScaleMode.COOKING, " onion ");
    WeightReading blank =
        new WeightReading(id, device, fridge, NOW, Grams.of(132), true, ScaleMode.FRIDGE, " ");

    assertEquals("onion", named.ingredientHint().orElseThrow());
    assertTrue(blank.ingredientHint().isEmpty());
  }

  @Test
  void everyEventRequiresIdentityAndPayload() {
    assertThrows(NullPointerException.class, () -> new SensorEventId(null));
    assertThrows(NullPointerException.class, () -> new DoorEvent(id, device, fridge, NOW, null));
    assertThrows(
        NullPointerException.class,
        () -> new TemperatureReading(id, device, null, NOW, BigDecimal.ONE));
    assertThrows(
        NullPointerException.class,
        () -> new WeightReading(id, device, fridge, NOW, Grams.ZERO, true, null, null));
    SensorEvent door = new DoorEvent(id, device, fridge, NOW, DoorState.OPEN);
    assertEquals(fridge, door.fridge());
  }
}
