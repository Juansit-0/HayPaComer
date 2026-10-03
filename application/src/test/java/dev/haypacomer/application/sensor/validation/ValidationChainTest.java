package dev.haypacomer.application.sensor.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import dev.haypacomer.domain.sensor.WeightReading;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidationChainTest {

  private static final Instant NOW = Instant.parse("2026-10-03T18:00:00Z");

  private final DeviceId device = DeviceId.newId();
  private final FridgeId fridge = FridgeId.newId();
  private final Set<SensorEventId> seen = new HashSet<>();
  private final Map<String, Instant> last = new HashMap<>();
  private final SensorEventLog log =
      new SensorEventLog() {
        @Override
        public boolean contains(SensorEventId id) {
          return seen.contains(id);
        }

        @Override
        public Optional<Instant> lastAccepted(DeviceId target, String type) {
          return Optional.ofNullable(last.get(target + type));
        }

        @Override
        public void accept(SensorEvent event, Instant receivedAt) {
          seen.add(event.id());
          last.merge(
              event.device() + SensorEventTypes.of(event),
              event.occurredAt(),
              (a, b) -> a.isAfter(b) ? a : b);
        }
      };
  private final ValidateSensorEvent validate =
      new ValidateSensorEvent(log, Clock.fixed(NOW, ZoneOffset.UTC));

  private SensorEventId id() {
    return new SensorEventId(UUID.randomUUID());
  }

  private TemperatureReading temperature(String celsius, Instant at) {
    return new TemperatureReading(id(), device, fridge, at, new BigDecimal(celsius));
  }

  private WeightReading weight(long grams, boolean stable, Instant at) {
    return new WeightReading(
        id(), device, fridge, at, Grams.of(grams), stable, ScaleMode.FRIDGE, null);
  }

  @Test
  void acceptsValidEventsOfEveryType() {
    assertTrue(validate.validate(temperature("4.5", NOW)).accepted());
    assertTrue(validate.validate(weight(650, true, NOW)).accepted());
    assertTrue(
        validate.validate(new DoorEvent(id(), device, fridge, NOW, DoorState.OPEN)).accepted());
  }

  @Test
  void rejectsOutOfRangeReadings() {
    assertEquals(Verdict.REJECTED, validate.validate(temperature("61", NOW)).verdict());
    assertEquals(Verdict.REJECTED, validate.validate(temperature("-30.5", NOW)).verdict());
    assertEquals(Verdict.REJECTED, validate.validate(weight(20001, true, NOW)).verdict());
    assertTrue(validate.validate(temperature("-30", NOW)).accepted());
  }

  @Test
  void rejectsSkewedTimestamps() {
    assertEquals(
        "timestamp is in the future",
        validate.validate(temperature("4", NOW.plus(Duration.ofMinutes(3)))).reason());
    assertEquals(
        "timestamp is too old",
        validate.validate(temperature("4", NOW.minus(Duration.ofDays(8)))).reason());
    assertTrue(validate.validate(temperature("4", NOW.minus(Duration.ofDays(6)))).accepted());
  }

  @Test
  void dropsUnstableWeights() {
    assertEquals(Verdict.DROPPED, validate.validate(weight(700, false, NOW)).verdict());
  }

  @Test
  void flagsDuplicatesAndDropsStaleReadings() {
    TemperatureReading first = temperature("4", NOW);
    log.accept(first, NOW);

    assertEquals(Verdict.DUPLICATE, validate.validate(first).verdict());
    assertEquals(
        Verdict.DROPPED, validate.validate(temperature("4", NOW.minusSeconds(30))).verdict());
    assertTrue(validate.validate(weight(650, true, NOW.minusSeconds(30))).accepted());
    assertTrue(validate.validate(temperature("4", NOW.plusSeconds(30))).accepted());
  }

  @Test
  void firstFailingLinkAnswersAndStopsTheChain() {
    TemperatureReading broken = temperature("99", NOW.plus(Duration.ofHours(1)));
    log.accept(broken, NOW);

    assertEquals("tempC outside [-30, 60]", validate.validate(broken).reason());
  }
}
