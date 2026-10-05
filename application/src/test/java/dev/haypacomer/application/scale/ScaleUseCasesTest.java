package dev.haypacomer.application.scale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.device.DeviceNotFoundException;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.ScaleCalibrationRepository;
import dev.haypacomer.application.port.ScaleSampleStore;
import dev.haypacomer.application.port.ScaleSessionStore;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.scale.ScaleCalibration;
import dev.haypacomer.domain.scale.WeighingProgress;
import dev.haypacomer.domain.scale.WeighingStatus;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.ScaleMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScaleUseCasesTest {

  private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final Map<DeviceId, Device> deviceStore = new HashMap<>();
  private final Map<DeviceId, RawSample> latest = new HashMap<>();
  private final Map<DeviceId, ScaleCalibration> calibrationStore = new HashMap<>();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household household;
  private Device scale;
  private Device door;

  private final DeviceRepository devices =
      new DeviceRepository() {
        @Override
        public void save(Device device) {
          deviceStore.put(device.id(), device);
        }

        @Override
        public Optional<Device> findById(DeviceId id) {
          return Optional.ofNullable(deviceStore.get(id));
        }

        @Override
        public Optional<Device> findByKeyHash(String hash) {
          return Optional.empty();
        }

        @Override
        public List<Device> findByHousehold(HouseholdId id) {
          return List.copyOf(deviceStore.values());
        }
      };

  private final ScaleSampleStore samples =
      new ScaleSampleStore() {
        @Override
        public void record(DeviceId device, RawSample sample) {
          latest.put(device, sample);
        }

        @Override
        public Optional<RawSample> latest(DeviceId device) {
          return Optional.ofNullable(latest.get(device));
        }

        @Override
        public boolean stable(DeviceId device, Grams grams, Instant at) {
          return true;
        }
      };

  private final ScaleCalibrationRepository calibrations =
      new ScaleCalibrationRepository() {
        @Override
        public Optional<ScaleCalibration> find(DeviceId device) {
          return Optional.ofNullable(calibrationStore.get(device));
        }

        @Override
        public void save(DeviceId device, ScaleCalibration calibration) {
          calibrationStore.put(device, calibration);
        }
      };

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    households.save(household);
    scale =
        Device.register(
            household.id(), FridgeId.newId(), "Scale", DeviceKind.ESP32_SCALE, "ab", NOW);
    door =
        Device.register(
            household.id(), FridgeId.newId(), "Door", DeviceKind.ESP32_DOOR_TEMP, "cd", NOW);
    devices.save(scale);
    devices.save(door);
  }

  private TareScale tare() {
    return new TareScale(households, devices, samples, calibrations, clock);
  }

  private CalibrateScale calibrate() {
    return new CalibrateScale(households, devices, samples, calibrations, clock);
  }

  private ReadScale read() {
    return new ReadScale(households, devices, samples, calibrations, clock);
  }

  @Test
  void tareCalibrateAndReadGrams() {
    samples.record(scale.id(), new RawSample(84_000, NOW.minusSeconds(2)));
    tare().tare(ana, household.id(), scale.id());
    assertFalse(read().read(ana, household.id(), scale.id()).calibrated());

    samples.record(scale.id(), new RawSample(298_000, NOW.minusSeconds(1)));
    assertThrows(
        AccessDeniedException.class,
        () -> calibrate().calibrate(ana, household.id(), scale.id(), Grams.of(500)));
    calibrate().calibrate(juan, household.id(), scale.id(), Grams.of(500));

    samples.record(scale.id(), new RawSample(84_000 + 428L * 842, NOW));
    ScaleReading reading = read().read(ana, household.id(), scale.id());
    assertTrue(reading.calibrated());
    assertEquals(Grams.of(842), reading.weight().orElseThrow());

    samples.record(scale.id(), new RawSample(84_000 + 428L * 50, NOW));
    tare().tare(ana, household.id(), scale.id());
    samples.record(scale.id(), new RawSample(84_000 + 428L * 180, NOW));
    assertEquals(
        Grams.of(130), read().read(ana, household.id(), scale.id()).weight().orElseThrow());
  }

  @Test
  void cookingModeComparesTheScaleWithTheTarget() {
    Map<DeviceId, WeighingTarget> cooking = new HashMap<>();
    ScaleSessionStore sessions =
        new ScaleSessionStore() {
          @Override
          public ScaleMode mode(DeviceId device) {
            return cooking.containsKey(device) ? ScaleMode.COOKING : ScaleMode.FRIDGE;
          }

          @Override
          public Optional<WeighingTarget> target(DeviceId device) {
            return Optional.ofNullable(cooking.get(device));
          }

          @Override
          public void cook(DeviceId device, WeighingTarget target) {
            cooking.put(device, target);
          }

          @Override
          public void fridge(DeviceId device) {
            cooking.remove(device);
          }
        };
    SetScaleMode setMode = new SetScaleMode(households, devices, samples, sessions, clock);
    ReadWeighingProgress progress =
        new ReadWeighingProgress(households, devices, samples, calibrations, sessions, clock);
    samples.record(scale.id(), new RawSample(0, NOW));
    tare().tare(juan, household.id(), scale.id());
    samples.record(scale.id(), new RawSample(428_000, NOW));
    calibrate().calibrate(juan, household.id(), scale.id(), Grams.of(1000));

    assertThrows(
        IllegalArgumentException.class,
        () -> setMode.set(ana, household.id(), scale.id(), ScaleMode.COOKING, Optional.empty()));
    assertThrows(IllegalStateException.class, () -> progress.read(ana, household.id(), scale.id()));
    assertEquals(
        ScaleMode.COOKING,
        setMode.set(
            ana,
            household.id(),
            scale.id(),
            ScaleMode.COOKING,
            Optional.of(WeighingTarget.of("Chicken breast", Grams.of(200)))));
    samples.record(scale.id(), new RawSample(428L * 80, NOW));

    WeighingProgress reading = progress.read(ana, household.id(), scale.id());

    assertEquals(WeighingStatus.SHORT, reading.status());
    assertEquals(Grams.of(120), reading.remaining());
    assertEquals(
        ScaleMode.FRIDGE,
        setMode.set(ana, household.id(), scale.id(), ScaleMode.FRIDGE, Optional.empty()));
  }

  @Test
  void needsAFreshSampleAndAPriorTare() {
    assertThrows(
        NoRecentSampleException.class, () -> tare().tare(juan, household.id(), scale.id()));
    assertThrows(
        NoRecentSampleException.class, () -> read().read(juan, household.id(), scale.id()));
    samples.record(scale.id(), new RawSample(1_000, NOW.minus(Duration.ofSeconds(30))));
    assertThrows(
        NoRecentSampleException.class, () -> tare().tare(juan, household.id(), scale.id()));
    assertEquals(1_000, read().read(juan, household.id(), scale.id()).rawCounts());
    samples.record(scale.id(), new RawSample(2_000, NOW));
    assertThrows(
        IllegalStateException.class,
        () -> calibrate().calibrate(juan, household.id(), scale.id(), Grams.of(500)));
  }

  @Test
  void onlyActiveScalesOfTheHouseholdCanBeUsed() {
    assertThrows(DeviceNotFoundException.class, () -> tare().tare(juan, household.id(), door.id()));
    assertThrows(
        DeviceNotFoundException.class, () -> tare().tare(juan, household.id(), DeviceId.newId()));
    devices.save(scale.revoke(NOW));
    assertThrows(
        DeviceNotFoundException.class, () -> read().read(juan, household.id(), scale.id()));
  }
}
