package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.scale.ScaleCalibration;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class PostgresScaleCalibrationRepositoryTest extends PostgresTestSupport {

  @Test
  void savesTareAndCalibration() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    Fridge fridge = Fridge.named("Kitchen");
    new PostgresFridgeRepository(dataSource).save(household.id(), fridge);
    byte[] key = new byte[32];
    key[0] = 3;
    Device scale =
        Device.register(
            household.id(),
            fridge.id(),
            "Scale",
            DeviceKind.ESP32_SCALE,
            HexFormat.of().formatHex(key),
            NOW);
    new PostgresDeviceRepository(dataSource).save(scale);
    PostgresScaleCalibrationRepository repository =
        new PostgresScaleCalibrationRepository(dataSource);

    ScaleCalibration tared = ScaleCalibration.taredAt(new RawSample(84_000, NOW));
    repository.save(scale.id(), tared);
    assertEquals(tared, repository.find(scale.id()).orElseThrow());

    ScaleCalibration calibrated =
        tared.calibrate(new RawSample(298_000, NOW.plusSeconds(5)), Grams.of(500));
    repository.save(scale.id(), calibrated);
    ScaleCalibration loaded = repository.find(scale.id()).orElseThrow();
    assertEquals(Grams.of(842), loaded.toGrams(84_000 + 428L * 842).orElseThrow());
    assertEquals(NOW.plusSeconds(5), loaded.calibratedAt());
    assertTrue(repository.find(DeviceId.newId()).isEmpty());
  }
}
