package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.scale.ScaleAssignment;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FoodItem;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.fridge.Zone;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.quantity.Grams;
import java.time.ZoneId;
import java.util.Currency;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class PostgresScaleAssignmentRepositoryTest extends PostgresTestSupport {

  @Test
  void assignsReplacesAndRemovesTheItemOnAScale() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    new PostgresFoodCatalogRepository(dataSource).save(MILK);
    Fridge fridge = Fridge.named("Kitchen");
    Zone door = Zone.named("Door", ZoneKind.DOOR);
    Tray rack = Tray.named("Rack", 0);
    door.add(rack);
    fridge.add(door);
    FoodItem first = new FoodItem(FoodItemId.newId(), MILK, Grams.of(842), Grams.of(50), null);
    FoodItem second = new FoodItem(FoodItemId.newId(), MILK, Grams.of(500), Grams.ZERO, null);
    fridge.place(first, rack.id());
    fridge.place(second, rack.id());
    new PostgresFridgeRepository(dataSource).save(household.id(), fridge);
    byte[] key = new byte[32];
    key[0] = 4;
    Device scale =
        Device.register(
            household.id(),
            fridge.id(),
            "Scale",
            DeviceKind.ESP32_SCALE,
            HexFormat.of().formatHex(key),
            NOW);
    new PostgresDeviceRepository(dataSource).save(scale);
    PostgresScaleAssignmentRepository assignments =
        new PostgresScaleAssignmentRepository(dataSource);

    assignments.save(new ScaleAssignment(scale.id(), household.id(), first.id(), juan.id(), NOW));
    ScaleAssignment replaced =
        new ScaleAssignment(scale.id(), household.id(), second.id(), juan.id(), NOW.plusSeconds(5));
    assignments.save(replaced);

    assertEquals(replaced, assignments.find(scale.id()).orElseThrow());
    assignments.remove(scale.id());
    assertTrue(assignments.find(scale.id()).isEmpty());
  }
}
