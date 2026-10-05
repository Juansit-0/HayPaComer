package dev.haypacomer.sensors.scale;

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
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.scale.ScaleCalibration;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.WeightReading;
import dev.haypacomer.sensors.esp32.Esp32EventAdapter;
import dev.haypacomer.sensors.esp32.Esp32Simulator;
import dev.haypacomer.sensors.esp32.Hx711ReadingAdapter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class Hx711ReadingAdapterTest {

  private static final Instant T0 = Instant.parse("2026-10-05T12:00:00Z");

  private final Device scale =
      Device.register(
          HouseholdId.newId(), FridgeId.newId(), "Scale", DeviceKind.ESP32_SCALE, "ab", T0);
  private final InMemoryCalibrations calibrations = new InMemoryCalibrations();
  private final InMemoryScaleSampleStore samples = new InMemoryScaleSampleStore();
  private final Esp32EventAdapter adapter =
      new Esp32EventAdapter(new Hx711ReadingAdapter(calibrations, samples));
  private final Esp32Simulator simulator = new Esp32Simulator("scale-01");

  private List<SensorEvent> send(long counts, long millis) {
    return adapter.decode(
        scale, simulator.toJson(simulator.rawWeight(counts, "FRIDGE", T0.plusMillis(millis))));
  }

  @Test
  void uncalibratedSamplesAreRecordedButNotEmitted() {
    assertTrue(send(84_000, 0).isEmpty());
    assertEquals(new RawSample(84_000, T0), samples.latest(scale.id()).orElseThrow());
  }

  @Test
  void convertsCountsToGramsAndDetectsStability() {
    calibrations.save(
        scale.id(),
        ScaleCalibration.taredAt(new RawSample(84_000, T0))
            .calibrate(new RawSample(298_000, T0), Grams.of(500)));
    long milk = 84_000 + 428L * 650;

    WeightReading first = assertInstanceOf(WeightReading.class, send(milk, 0).getFirst());
    WeightReading later = assertInstanceOf(WeightReading.class, send(milk + 200, 1_100).getFirst());

    assertEquals(Grams.of(650), first.grams());
    assertFalse(first.stable());
    assertTrue(later.stable());
    assertEquals(ScaleMode.FRIDGE, later.mode());
  }

  @Test
  void trustsStabilityReportedByTheDevice() {
    calibrations.save(
        scale.id(),
        ScaleCalibration.taredAt(new RawSample(0, T0))
            .calibrate(new RawSample(1_000, T0), Grams.of(1)));
    String payload =
        "{\"eventId\":\"7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77\",\"type\":\"RAW_WEIGHT\",\"raw\":132000,"
            + "\"stable\":true,\"mode\":\"COOK\",\"ingredient\":\"onion\",\"at\":\"2026-10-05T12:00:00Z\"}";

    WeightReading reading =
        assertInstanceOf(WeightReading.class, adapter.decode(scale, payload).getFirst());

    assertEquals(Grams.of(132), reading.grams());
    assertTrue(reading.stable());
    assertEquals(ScaleMode.COOKING, reading.mode());
    assertEquals("onion", reading.ingredientHint().orElseThrow());
  }

  @Test
  void rejectsIncompleteRawEnvelopes() {
    String noRaw =
        "{\"eventId\":\"7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77\",\"type\":\"RAW_WEIGHT\",\"at\":\"2026-10-05T12:00:00Z\"}";
    String badMode =
        "{\"eventId\":\"7f9c2b1e-9d3a-4c5f-8e2b-1a6d4f0c9b77\",\"type\":\"RAW_WEIGHT\",\"raw\":1,\"mode\":\"X\",\"at\":\"2026-10-05T12:00:00Z\"}";

    assertThrows(MalformedSensorPayloadException.class, () -> adapter.decode(scale, noRaw));
    assertThrows(MalformedSensorPayloadException.class, () -> adapter.decode(scale, badMode));
    assertThrows(
        MalformedSensorPayloadException.class,
        () -> adapter.decode(scale, "{\"type\":\"RAW_WEIGHT\",\"raw\":1}"));
    assertThrows(
        MalformedSensorPayloadException.class,
        () ->
            new Esp32EventAdapter()
                .decode(scale, simulator.toJson(simulator.rawWeight(1, "FRIDGE", T0))));
  }
}
