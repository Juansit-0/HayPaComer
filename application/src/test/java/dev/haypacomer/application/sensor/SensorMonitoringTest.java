package dev.haypacomer.application.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.port.AlertSignal;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.application.port.SensorEventDecoder;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FindingKind;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.TemperatureReading;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SensorMonitoringTest {

  private static final Instant T0 = Instant.parse("2026-10-03T18:00:00Z");

  private final List<AlertPattern> buzzer = new ArrayList<>();
  private final MutableClock clock = new MutableClock();
  private Device device;
  private ObserveSensorEvent observe;
  private CheckFridgeAlerts check;

  private static final class MutableClock extends Clock {
    Instant now = T0;

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private static final class Registry implements FridgeMonitorRegistry {
    final Map<FridgeId, FridgeMonitor> monitors = new HashMap<>();
    final Map<FridgeId, DeviceId> devices = new HashMap<>();
    final Set<String> reported = new HashSet<>();

    @Override
    public FridgeMonitor monitor(FridgeId fridge) {
      return monitors.computeIfAbsent(
          fridge, id -> new FridgeMonitor(id, FridgeThresholds.DEFAULT));
    }

    @Override
    public void rememberDevice(FridgeId fridge, DeviceId device) {
      devices.put(fridge, device);
    }

    @Override
    public Optional<DeviceId> lastDevice(FridgeId fridge) {
      return Optional.ofNullable(devices.get(fridge));
    }

    @Override
    public Collection<FridgeId> fridges() {
      return monitors.keySet();
    }

    @Override
    public boolean firstReport(FridgeId fridge, Finding finding) {
      return reported.add(fridge + "|" + finding.kind() + "|" + finding.since());
    }
  }

  @BeforeEach
  void wire() {
    device =
        Device.register(
            HouseholdId.newId(), FridgeId.newId(), "Door", DeviceKind.ESP32_DOOR_TEMP, "ab", T0);
    DeviceRepository devices =
        new DeviceRepository() {
          @Override
          public void save(Device value) {}

          @Override
          public Optional<Device> findById(DeviceId id) {
            return id.equals(device.id()) ? Optional.of(device) : Optional.empty();
          }

          @Override
          public Optional<Device> findByKeyHash(String apiKeyHash) {
            return Optional.empty();
          }

          @Override
          public List<Device> findByHousehold(HouseholdId household) {
            return List.of();
          }
        };
    AlertSignal signal =
        new AlertSignal() {
          @Override
          public void signal(DeviceId target, AlertPattern pattern) {
            buzzer.add(pattern);
          }

          @Override
          public List<AlertPattern> drain(DeviceId target) {
            return List.of();
          }
        };
    HardwareFactory family =
        new HardwareFactory() {
          @Override
          public boolean supports(DeviceKind kind) {
            return true;
          }

          @Override
          public SensorEventDecoder decoder() {
            return null;
          }

          @Override
          public AlertSignal alerts() {
            return signal;
          }
        };
    HardwareFactories hardware = new HardwareFactories(List.of(family));
    Registry registry = new Registry();
    observe = new ObserveSensorEvent(registry, devices, hardware, clock);
    check = new CheckFridgeAlerts(registry, devices, hardware, clock);
  }

  private DoorEvent door(DoorState state, long seconds) {
    return new DoorEvent(
        new SensorEventId(UUID.randomUUID()),
        device.id(),
        device.fridge(),
        T0.plusSeconds(seconds),
        state);
  }

  @Test
  void doorLeftOpenBeepsOncePerEpisodeFromThePeriodicCheck() {
    assertTrue(observe.observe(device, door(DoorState.OPEN, 0)).isEmpty());

    clock.now = T0.plusSeconds(30);
    assertTrue(check.check().isEmpty());
    clock.now = T0.plusSeconds(41);
    assertEquals(FindingKind.DOOR_LEFT_OPEN, check.check().getFirst().kind());
    clock.now = T0.plusSeconds(50);
    assertTrue(check.check().isEmpty());
    assertEquals(List.of(AlertPattern.DOOR_OPEN_BEEP), buzzer);

    observe.observe(device, door(DoorState.CLOSED, 55));
    observe.observe(device, door(DoorState.OPEN, 100));
    clock.now = T0.plusSeconds(145);
    check.check();
    assertEquals(List.of(AlertPattern.DOOR_OPEN_BEEP, AlertPattern.DOOR_OPEN_BEEP), buzzer);
  }

  @Test
  void bufferedEventsAreEvaluatedAtTheirOwnTime() {
    clock.now = T0;
    observe.observe(device, door(DoorState.OPEN, 0));

    List<Finding> findings =
        observe.observe(
            device,
            new TemperatureReading(
                new SensorEventId(UUID.randomUUID()),
                device.id(),
                device.fridge(),
                T0.plus(Duration.ofMinutes(2)),
                BigDecimal.ONE));

    assertEquals(FindingKind.DOOR_LEFT_OPEN, findings.getFirst().kind());
  }

  @Test
  void rejectsEventsFromAnotherDevice() {
    DoorEvent foreign =
        new DoorEvent(
            new SensorEventId(UUID.randomUUID()),
            DeviceId.newId(),
            device.fridge(),
            T0,
            DoorState.OPEN);

    assertThrows(IllegalArgumentException.class, () -> observe.observe(device, foreign));
  }
}
