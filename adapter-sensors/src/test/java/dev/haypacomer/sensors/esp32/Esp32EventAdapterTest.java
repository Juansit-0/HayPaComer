package dev.haypacomer.sensors.esp32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.TemperatureReading;
import dev.haypacomer.domain.sensor.WeightReading;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class Esp32EventAdapterTest {

  private static final Instant AT = Instant.parse("2026-09-04T18:40:00Z");

  private final Device device =
      Device.register(
          HouseholdId.newId(), FridgeId.newId(), "Door", DeviceKind.ESP32_DOOR_TEMP, "ab", AT);
  private final Esp32EventAdapter adapter = new Esp32EventAdapter();
  private final Esp32Simulator simulator = new Esp32Simulator("fridge-01");

  @Test
  void adaptsTheDocumentedWeightEnvelope() {
    String payload =
        """
        {"eventId":"7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77","device":"fridge-01","type":"WEIGHT",
         "mode":"COOK","door":"CLOSED","tempC":5.2,"grams":132,"stable":true,
         "ingredient":"onion","at":"2026-09-04T18:40:00Z","firmware":"1.4.2"}
        """;

    WeightReading reading =
        assertInstanceOf(WeightReading.class, adapter.decode(device, payload).getFirst());

    assertEquals("7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77", reading.id().value().toString());
    assertEquals(device.id(), reading.device());
    assertEquals(device.fridge(), reading.fridge());
    assertEquals(Grams.of(132), reading.grams());
    assertEquals(ScaleMode.COOKING, reading.mode());
    assertEquals("onion", reading.ingredientHint().orElseThrow());
    assertEquals(AT, reading.occurredAt());
  }

  @Test
  void adaptsDoorAndTemperatureEventsFromTheSimulator() {
    DoorEvent door =
        assertInstanceOf(
            DoorEvent.class,
            adapter.decode(device, simulator.toJson(simulator.door(true, AT))).getFirst());
    TemperatureReading temperature =
        assertInstanceOf(
            TemperatureReading.class,
            adapter.decode(device, simulator.toJson(simulator.temperature("4.5", AT))).getFirst());

    assertEquals(DoorState.OPEN, door.state());
    assertEquals(0, new BigDecimal("4.5").compareTo(temperature.celsius()));
  }

  @Test
  void decodesBufferedBatchesInOrder() {
    List<SensorEvent> events =
        adapter.decode(device, simulator.toJson(simulator.productRemoval("842", "650", AT)));

    assertEquals(5, events.size());
    WeightReading after = assertInstanceOf(WeightReading.class, events.get(3));
    assertTrue(after.stable());
    assertEquals(Grams.of(650), after.grams());
    assertFalse(assertInstanceOf(WeightReading.class, events.get(2)).stable());
    assertEquals(
        List.of(DoorState.OPEN, DoorState.CLOSED),
        adapter
            .decode(device, simulator.toJson(simulator.doorLeftOpen(AT, Duration.ofSeconds(40))))
            .stream()
            .map(event -> ((DoorEvent) event).state())
            .toList());
    assertEquals(
        3,
        adapter
            .decode(
                device,
                simulator.toJson(
                    simulator.coldChainBreak(AT, Duration.ofMinutes(5), "4", "9", "12")))
            .size());
  }

  @Test
  void defaultsScaleModeToFridgeAndAcceptsLowercase() {
    String payload =
        "{\"eventId\":\"7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77\",\"type\":\"weight\",\"grams\":10,"
            + "\"stable\":true,\"at\":\"2026-09-04T18:40:00Z\"}";

    assertEquals(
        ScaleMode.FRIDGE, ((WeightReading) adapter.decode(device, payload).getFirst()).mode());
  }

  @Test
  void rejectsMalformedPayloads() {
    String base =
        "\"eventId\":\"7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77\",\"at\":\"2026-09-04T18:40:00Z\"";
    List<String> invalid =
        List.of(
            "",
            "not json",
            "42",
            "[]",
            "{\"type\":\"DOOR\",\"door\":\"OPEN\",\"at\":\"2026-09-04T18:40:00Z\"}",
            "{\"type\":\"DOOR\",\"door\":\"OPEN\",\"eventId\":\"7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77\"}",
            "{" + base + ",\"type\":\"DOOR\",\"door\":\"AJAR\"}",
            "{" + base + ",\"type\":\"DOOR\"}",
            "{" + base + ",\"type\":\"TEMPERATURE\"}",
            "{" + base + ",\"type\":\"WEIGHT\",\"grams\":10}",
            "{" + base + ",\"type\":\"WEIGHT\",\"grams\":-1,\"stable\":true}",
            "{" + base + ",\"type\":\"WEIGHT\",\"grams\":10,\"stable\":true,\"mode\":\"BLENDER\"}",
            "{" + base + ",\"type\":\"HUMIDITY\"}",
            "{\"eventId\":\"not-a-uuid\",\"type\":\"DOOR\",\"door\":\"OPEN\"}");

    invalid.forEach(
        payload ->
            assertThrows(
                MalformedSensorPayloadException.class,
                () -> adapter.decode(device, payload),
                payload));
    assertThrows(MalformedSensorPayloadException.class, () -> adapter.decode(device, null));
  }
}
