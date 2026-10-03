package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import dev.haypacomer.domain.sensor.WeightReading;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostgresSensorEventLogTest extends PostgresTestSupport {

  @Test
  void storesEventsOnceAndTracksTheLatestPerType() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    Fridge fridge = Fridge.named("Kitchen");
    new PostgresFridgeRepository(dataSource).save(household.id(), fridge);
    byte[] key = new byte[32];
    key[0] = 9;
    Device device =
        Device.register(
            household.id(),
            fridge.id(),
            "Door",
            DeviceKind.ESP32_DOOR_TEMP,
            HexFormat.of().formatHex(key),
            NOW);
    new PostgresDeviceRepository(dataSource).save(device);
    PostgresSensorEventLog log = new PostgresSensorEventLog(dataSource);
    DoorEvent open =
        new DoorEvent(
            new SensorEventId(UUID.randomUUID()), device.id(), fridge.id(), NOW, DoorState.OPEN);

    assertFalse(log.contains(open.id()));
    assertTrue(log.lastAccepted(device.id(), "DOOR").isEmpty());

    log.accept(open, NOW);
    log.accept(open, NOW);
    log.accept(
        new TemperatureReading(
            new SensorEventId(UUID.randomUUID()),
            device.id(),
            fridge.id(),
            NOW.plusSeconds(30),
            new BigDecimal("4.5")),
        NOW);
    log.accept(
        new WeightReading(
            new SensorEventId(UUID.randomUUID()),
            device.id(),
            fridge.id(),
            NOW.plusSeconds(60),
            Grams.of(650),
            true,
            ScaleMode.FRIDGE,
            null),
        NOW);

    assertTrue(log.contains(open.id()));
    assertEquals(NOW, log.lastAccepted(device.id(), "DOOR").orElseThrow());
    assertEquals(NOW.plusSeconds(30), log.lastAccepted(device.id(), "TEMPERATURE").orElseThrow());
    assertEquals(NOW.plusSeconds(60), log.lastAccepted(device.id(), "WEIGHT").orElseThrow());
  }
}
