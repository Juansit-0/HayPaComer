package dev.haypacomer.application.scale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.fridge.FridgeLayout;
import dev.haypacomer.application.fridge.SetUpFridge;
import dev.haypacomer.application.inventory.CommandOutcome;
import dev.haypacomer.application.inventory.ExecuteInventoryCommand;
import dev.haypacomer.application.inventory.FoodAccessGuard;
import dev.haypacomer.application.inventory.FoodItemNotFoundException;
import dev.haypacomer.application.inventory.StockFoodCommand;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.ScaleAssignmentRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.application.support.InMemoryFridgeRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.application.support.InMemorySnapshotStore;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.fridge.Fridge;
import dev.haypacomer.domain.fridge.Tray;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.WeightReading;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FridgeModeDiscountTest {

  private static final Instant NOW = Instant.parse("2026-10-05T18:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryFridgeRepository fridges = new InMemoryFridgeRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final Map<DeviceId, ScaleAssignment> assignmentStore = new HashMap<>();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId guest = UserId.newId();
  private Household household;
  private Fridge fridge;
  private Device scale;
  private ExecuteInventoryCommand commands;
  private FoodItemId milk;

  private final ScaleAssignmentRepository assignments =
      new ScaleAssignmentRepository() {
        @Override
        public void save(ScaleAssignment assignment) {
          assignmentStore.put(assignment.scale(), assignment);
        }

        @Override
        public Optional<ScaleAssignment> find(DeviceId device) {
          return Optional.ofNullable(assignmentStore.get(device));
        }

        @Override
        public void remove(DeviceId device) {
          assignmentStore.remove(device);
        }
      };

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(guest, Role.GUEST, NOW);
    households.save(household);
    fridge =
        new SetUpFridge(households, fridges)
            .setUp(juan, household.id(), "Kitchen", FridgeLayout.STANDARD);
    Tray rack = fridge.trays().filter(tray -> tray.name().equals("Rack")).findFirst().orElseThrow();
    stores.catalog.save(
        new FoodMetadata(
            "Milk", FoodCategory.DAIRY, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 7, Set.of()));
    commands =
        new ExecuteInventoryCommand(
            households,
            fridges,
            stores.ownerships,
            stores.movements,
            stores.catalog,
            new FoodAccessGuard(),
            stores.audit,
            stores.unitOfWork,
            new InMemorySnapshotStore(),
            clock);
    milk =
        commands
            .execute(
                juan,
                new StockFoodCommand(
                    UUID.randomUUID(),
                    household.id(),
                    fridge.id(),
                    rack.id(),
                    "Milk",
                    Grams.of(892),
                    Grams.of(50),
                    null,
                    Visibility.SHARED))
            .item();
    scale =
        Device.register(household.id(), fridge.id(), "Scale", DeviceKind.ESP32_SCALE, "ab", NOW);
  }

  private AssignScaleItem assign() {
    DeviceRepository devices =
        new DeviceRepository() {
          @Override
          public void save(Device device) {}

          @Override
          public Optional<Device> findById(DeviceId id) {
            return id.equals(scale.id()) ? Optional.of(scale) : Optional.empty();
          }

          @Override
          public Optional<Device> findByKeyHash(String hash) {
            return Optional.empty();
          }

          @Override
          public List<Device> findByHousehold(HouseholdId id) {
            return List.of(scale);
          }
        };
    ScaleSampleStore samples =
        new ScaleSampleStore() {
          @Override
          public void record(DeviceId device, RawSample sample) {}

          @Override
          public Optional<RawSample> latest(DeviceId device) {
            return Optional.empty();
          }

          @Override
          public boolean stable(DeviceId device, Grams grams, Instant at) {
            return true;
          }
        };
    return new AssignScaleItem(
        households,
        devices,
        samples,
        fridges,
        stores.ownerships,
        new FoodAccessGuard(),
        assignments,
        clock);
  }

  private ApplyFridgeScaleReading discount() {
    return new ApplyFridgeScaleReading(assignments, fridges, commands, Grams.of(5));
  }

  private WeightReading reading(long gross, boolean stable, ScaleMode mode) {
    return new WeightReading(
        new SensorEventId(UUID.randomUUID()),
        scale.id(),
        fridge.id(),
        NOW,
        Grams.of(gross),
        stable,
        mode,
        null);
  }

  private Grams quantity() {
    return fridge.findItem(milk).map(item -> item.quantity()).orElse(Grams.ZERO);
  }

  @Test
  void stableFridgeReadingsDiscountTheMeasuredGrams() {
    assign().assign(juan, household.id(), scale.id(), milk);

    CommandOutcome outcome =
        discount().apply(scale, reading(700, true, ScaleMode.FRIDGE)).orElseThrow();

    assertEquals(Grams.of(650), outcome.remaining());
    assertEquals(Grams.of(650), quantity());
    assertEquals(MovementSource.SCALE, stores.movementLog.getLast().source());
    assertEquals(MovementType.CONSUME, stores.movementLog.getLast().type());
  }

  @Test
  void ignoresNoiseCookingModeUnstableReadingsAndUnassignedScales() {
    assertTrue(discount().apply(scale, reading(700, true, ScaleMode.FRIDGE)).isEmpty());
    assign().assign(juan, household.id(), scale.id(), milk);

    assertTrue(discount().apply(scale, reading(889, true, ScaleMode.FRIDGE)).isEmpty());
    assertTrue(discount().apply(scale, reading(700, false, ScaleMode.FRIDGE)).isEmpty());
    assertTrue(discount().apply(scale, reading(700, true, ScaleMode.COOKING)).isEmpty());
    assertTrue(discount().apply(scale, reading(990, true, ScaleMode.FRIDGE)).isEmpty());
    assertEquals(Grams.of(842), quantity());
  }

  @Test
  void anEmptyScaleConsumesTheWholeItemAndReplaysOnce() {
    assign().assign(juan, household.id(), scale.id(), milk);
    WeightReading empty = reading(10, true, ScaleMode.FRIDGE);

    assertEquals(Grams.ZERO, discount().apply(scale, empty).orElseThrow().remaining());
    assertTrue(discount().apply(scale, empty).isEmpty());
    assertTrue(fridge.findItem(milk).isEmpty());
  }

  @Test
  void assignmentRequiresAUsableItemOfTheHousehold() {
    assertThrows(
        FoodItemNotFoundException.class,
        () -> assign().assign(juan, household.id(), scale.id(), FoodItemId.newId()));
    assertThrows(
        AccessDeniedException.class,
        () -> assign().assign(guest, household.id(), scale.id(), milk));
    assertEquals(juan, assign().assign(juan, household.id(), scale.id(), milk).assignedBy());
  }
}
