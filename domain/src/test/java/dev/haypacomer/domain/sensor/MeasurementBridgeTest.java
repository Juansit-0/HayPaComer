package dev.haypacomer.domain.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeasurementBridgeTest {

  private static final Instant T0 = Instant.parse("2026-10-03T18:00:00Z");
  private final DeviceId device = DeviceId.newId();
  private final FridgeId fridge = FridgeId.newId();

  private SensorEventId id() {
    return new SensorEventId(UUID.randomUUID());
  }

  private DoorEvent door(DoorState state, long seconds) {
    return new DoorEvent(id(), device, fridge, T0.plusSeconds(seconds), state);
  }

  private TemperatureReading temperature(String celsius, long minutes) {
    return new TemperatureReading(
        id(), device, fridge, T0.plus(Duration.ofMinutes(minutes)), new BigDecimal(celsius));
  }

  private WeightReading weight(long grams, boolean stable, ScaleMode mode, long seconds) {
    return new WeightReading(
        id(), device, fridge, T0.plusSeconds(seconds), Grams.of(grams), stable, mode, null);
  }

  @Test
  void sameThresholdAbstractionWorksOnDoorAndTemperatureChannels() {
    DoorChannel doorChannel = new DoorChannel();
    TemperatureChannel temperatureChannel = new TemperatureChannel();
    Interpretation doorOpen =
        new SustainedThreshold(
            doorChannel, new BigDecimal("0.5"), Duration.ofSeconds(40), FindingKind.DOOR_LEFT_OPEN);
    Interpretation warm =
        new SustainedThreshold(
            temperatureChannel,
            new BigDecimal("5"),
            Duration.ofMinutes(20),
            FindingKind.COLD_CHAIN_BREACH);

    doorChannel.record(door(DoorState.OPEN, 0));
    temperatureChannel.record(temperature("4", 0));
    temperatureChannel.record(temperature("7", 5));
    temperatureChannel.record(temperature("9.5", 15));

    assertTrue(doorOpen.evaluate(T0.plusSeconds(39)).isEmpty());
    Finding open = doorOpen.evaluate(T0.plusSeconds(40)).orElseThrow();
    assertEquals(Duration.ofSeconds(40), open.duration());
    assertEquals("door", open.channel());
    assertTrue(warm.evaluate(T0.plus(Duration.ofMinutes(24))).isEmpty());
    Finding breach = warm.evaluate(T0.plus(Duration.ofMinutes(25))).orElseThrow();
    assertEquals(T0.plus(Duration.ofMinutes(5)), breach.since());
    assertEquals(0, new BigDecimal("9.5").compareTo(breach.magnitude()));
    assertEquals(temperatureChannel, warm.channel());

    doorChannel.record(door(DoorState.CLOSED, 41));
    assertTrue(doorOpen.evaluate(T0.plusSeconds(90)).isEmpty());
  }

  @Test
  void stableDeltaReadsStockChangesFromTheScale() {
    WeightChannel scale = new WeightChannel(ScaleMode.FRIDGE);
    Interpretation delta = new StableDelta(scale, new BigDecimal("5"));

    scale.record(weight(842, true, ScaleMode.FRIDGE, 0));
    assertTrue(delta.evaluate(T0).isEmpty());
    scale.record(weight(700, false, ScaleMode.FRIDGE, 5));
    scale.record(weight(650, true, ScaleMode.FRIDGE, 6));
    scale.record(weight(400, true, ScaleMode.COOKING, 7));

    Finding removed = delta.evaluate(T0.plusSeconds(6)).orElseThrow();
    assertEquals(FindingKind.STOCK_DECREASE, removed.kind());
    assertEquals(0, new BigDecimal("192").compareTo(removed.magnitude()));
    assertEquals("weight-fridge", scale.name());

    scale.record(weight(652, true, ScaleMode.FRIDGE, 9));
    assertTrue(delta.evaluate(T0.plusSeconds(9)).isEmpty());
    scale.record(weight(1150, true, ScaleMode.FRIDGE, 20));
    assertEquals(
        FindingKind.STOCK_INCREASE, delta.evaluate(T0.plusSeconds(20)).orElseThrow().kind());
  }

  @Test
  void channelsIgnoreForeignEventsAndKeepOrder() {
    DoorChannel channel = new DoorChannel();

    assertFalse(channel.record(temperature("4", 0)));
    assertTrue(channel.record(door(DoorState.CLOSED, 10)));
    assertTrue(channel.record(door(DoorState.OPEN, 5)));

    assertEquals(BigDecimal.ZERO, channel.latest().orElseThrow().value());
    assertEquals(2, channel.history().size());
    assertTrue(new DoorChannel().latest().isEmpty());
  }

  @Test
  void channelsDropReadingsOlderThanTheRetentionWindow() {
    TemperatureChannel channel = new TemperatureChannel();
    channel.record(temperature("4", 0));
    channel.record(temperature("4", 60 * 7));

    assertEquals(1, channel.history().size());
  }

  @Test
  void fridgeMonitorCombinesEveryInterpretation() {
    FridgeMonitor monitor = new FridgeMonitor(fridge, FridgeThresholds.DEFAULT);

    monitor.record(door(DoorState.OPEN, 0));
    monitor.record(weight(842, true, ScaleMode.FRIDGE, 1));
    monitor.record(weight(650, true, ScaleMode.FRIDGE, 30));

    List<FindingKind> kinds =
        monitor.evaluate(T0.plusSeconds(45)).stream().map(Finding::kind).toList();

    assertEquals(List.of(FindingKind.DOOR_LEFT_OPEN, FindingKind.STOCK_DECREASE), kinds);
    assertEquals(fridge, monitor.fridge());
    assertThrows(
        IllegalArgumentException.class,
        () -> monitor.record(new DoorEvent(id(), device, FridgeId.newId(), T0, DoorState.OPEN)));
  }
}
