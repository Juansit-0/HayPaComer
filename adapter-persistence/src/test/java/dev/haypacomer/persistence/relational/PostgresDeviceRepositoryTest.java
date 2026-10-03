package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostgresDeviceRepositoryTest extends PostgresTestSupport {

  @Test
  void savesFindsTouchesAndRevokesDevices() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    Fridge fridge = Fridge.named("Kitchen fridge");
    new PostgresFridgeRepository(dataSource).save(household.id(), fridge);
    PostgresDeviceRepository devices = new PostgresDeviceRepository(dataSource);
    byte[] bytes = new byte[32];
    bytes[0] = 7;
    String hash = HexFormat.of().formatHex(bytes);

    Device device =
        Device.register(household.id(), fridge.id(), "Door", DeviceKind.ESP32_DOOR_TEMP, hash, NOW);
    devices.save(device);

    assertEquals(device, devices.findByKeyHash(hash).orElseThrow());
    assertEquals(List.of(device), devices.findByHousehold(household.id()));

    devices.save(device.seenAt(NOW.plusSeconds(60)).revoke(NOW.plusSeconds(120)));

    Device loaded = devices.findById(device.id()).orElseThrow();
    assertEquals(NOW.plusSeconds(60), loaded.lastSeen().orElseThrow());
    assertFalse(loaded.isActive());
    assertTrue(devices.findById(DeviceId.newId()).isEmpty());
  }
}
