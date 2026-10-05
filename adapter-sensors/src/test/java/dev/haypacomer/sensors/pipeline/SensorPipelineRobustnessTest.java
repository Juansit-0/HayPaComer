package dev.haypacomer.sensors.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.application.sensor.AlertPattern;
import dev.haypacomer.application.sensor.CheckFridgeAlerts;
import dev.haypacomer.application.sensor.EventResult;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.application.sensor.IngestSensorEvents;
import dev.haypacomer.application.sensor.IngestionReport;
import dev.haypacomer.application.sensor.MalformedSensorPayloadException;
import dev.haypacomer.application.sensor.ObserveSensorEvent;
import dev.haypacomer.application.sensor.validation.SensorEventTypes;
import dev.haypacomer.application.sensor.validation.ValidateSensorEvent;
import dev.haypacomer.application.sensor.validation.Verdict;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.coldchain.ColdChainPhase;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.sensor.FindingKind;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.sensors.esp32.Esp32Envelope;
import dev.haypacomer.sensors.esp32.Esp32Simulator;
import dev.haypacomer.sensors.hardware.Esp32HardwareFactory;
import dev.haypacomer.sensors.hardware.SimulatedHardwareFactory;
import dev.haypacomer.sensors.monitor.InMemoryFridgeMonitorRegistry;
import dev.haypacomer.sensors.scale.InMemoryCalibrations;
import dev.haypacomer.sensors.scale.InMemoryScaleSampleStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SensorPipelineRobustnessTest {

  private static final Instant T0 = Instant.parse("2026-10-03T18:00:00Z");

  private final MovableClock clock = new MovableClock(T0);
  private final Esp32Simulator simulator = new Esp32Simulator("fridge-01");
  private final Map<SensorEventId, SensorEvent> stored = new HashMap<>();
  private final Map<FridgeId, ColdChain> chains = new HashMap<>();
  private Device device;
  private HardwareFactories hardware;
  private IngestSensorEvents ingest;
  private CheckFridgeAlerts check;

  private static final class MovableClock extends Clock {
    private Instant now;

    MovableClock(Instant now) {
      this.now = now;
    }

    void set(Instant instant) {
      now = instant;
    }

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

  @BeforeEach
  void wirePipeline() {
    device =
        Device.register(
            HouseholdId.newId(), FridgeId.newId(), "Door", DeviceKind.ESP32_DOOR_TEMP, "ab", T0);
    hardware =
        new HardwareFactories(
            List.of(
                new Esp32HardwareFactory(
                    new InMemoryCalibrations(), new InMemoryScaleSampleStore()),
                new SimulatedHardwareFactory(
                    clock, new InMemoryCalibrations(), new InMemoryScaleSampleStore())));
    SensorEventLog log =
        new SensorEventLog() {
          @Override
          public boolean contains(SensorEventId id) {
            return stored.containsKey(id);
          }

          @Override
          public Optional<Instant> lastAccepted(DeviceId target, String type) {
            return stored.values().stream()
                .filter(
                    event ->
                        event.device().equals(target) && SensorEventTypes.of(event).equals(type))
                .map(SensorEvent::occurredAt)
                .max(Instant::compareTo);
          }

          @Override
          public void accept(SensorEvent event, Instant receivedAt) {
            stored.put(event.id(), event);
          }
        };
    ColdChainRepository coldChains =
        new ColdChainRepository() {
          @Override
          public Optional<ColdChain> find(FridgeId fridge) {
            return Optional.ofNullable(chains.get(fridge));
          }

          @Override
          public void save(ColdChain chain) {
            chains.put(chain.fridge(), chain);
          }
        };
    DeviceRepository devices =
        new DeviceRepository() {
          @Override
          public void save(Device value) {}

          @Override
          public Optional<Device> findById(DeviceId id) {
            return Optional.of(device);
          }

          @Override
          public Optional<Device> findByKeyHash(String hash) {
            return Optional.empty();
          }

          @Override
          public List<Device> findByHousehold(HouseholdId household) {
            return List.of();
          }
        };
    InMemoryFridgeMonitorRegistry registry =
        new InMemoryFridgeMonitorRegistry(FridgeThresholds.DEFAULT);
    ingest =
        new IngestSensorEvents(
            hardware,
            new ValidateSensorEvent(log, clock),
            log,
            new ObserveSensorEvent(registry, devices, hardware),
            new TrackColdChain(coldChains, FridgeThresholds.DEFAULT),
            (scale, reading) -> Optional.empty(),
            clock);
    check = new CheckFridgeAlerts(registry, devices, hardware, clock);
  }

  private IngestionReport send(List<Esp32Envelope> envelopes) {
    return ingest.ingest(device, simulator.toJson(envelopes));
  }

  private List<AlertPattern> buzzer() {
    return hardware.forDevice(device).alerts().drain(device.id());
  }

  private ColdChainPhase coldChain() {
    return Optional.ofNullable(chains.get(device.fridge()))
        .map(ColdChain::phase)
        .orElse(ColdChainPhase.NORMAL);
  }

  @Test
  void anIsolatedTemperatureSpikeIsNoise() {
    clock.set(T0.plus(Duration.ofMinutes(30)));

    send(simulator.coldChainBreak(T0, Duration.ofMinutes(5), "4", "12", "4", "4.2", "3.9"));

    assertEquals(ColdChainPhase.NORMAL, coldChain());
    assertTrue(check.check().isEmpty());
    assertTrue(buzzer().isEmpty());
  }

  @Test
  void coldChainBoundariesAreExact() {
    clock.set(T0.plus(Duration.ofMinutes(19)));
    send(simulator.coldChainBreak(T0, Duration.ofMinutes(19), "5.0", "5.0"));
    assertEquals(ColdChainPhase.NORMAL, coldChain());

    clock.set(T0.plus(Duration.ofMinutes(60)));
    send(
        simulator.coldChainBreak(
            T0.plus(Duration.ofMinutes(20)), Duration.ofSeconds(1199), "5.1", "5.1"));
    assertEquals(ColdChainPhase.WARMING, coldChain());
    send(List.of(simulator.temperature("5.1", T0.plus(Duration.ofMinutes(40)))));
    assertEquals(ColdChainPhase.UNDER_REVIEW, coldChain());
  }

  @Test
  void doorBounceAndTheFortySecondBoundary() {
    clock.set(T0.plusSeconds(1));
    List<Esp32Envelope> bounce = new ArrayList<>();
    for (int index = 0; index < 6; index++) {
      bounce.add(simulator.door(index % 2 == 0, T0.plusMillis(index * 30L)));
    }
    bounce.add(simulator.door(true, T0.plusSeconds(1)));
    send(bounce);

    clock.set(T0.plusSeconds(40));
    assertTrue(check.check().isEmpty());
    clock.set(T0.plusSeconds(41));
    assertEquals(FindingKind.DOOR_LEFT_OPEN, check.check().getFirst().kind());
    assertEquals(List.of(AlertPattern.DOOR_OPEN_BEEP), buzzer());
    clock.set(T0.plusSeconds(90));
    assertTrue(check.check().isEmpty());
  }

  @Test
  void duplicatesInsideAndAcrossBatchesApplyOnce() {
    clock.set(T0.plusSeconds(10));
    Esp32Envelope open = simulator.door(true, T0);
    Esp32Envelope closed = simulator.door(false, T0.plusSeconds(5));

    IngestionReport first = send(List.of(open, open, closed));
    IngestionReport retry = send(List.of(open, closed));

    assertEquals(
        List.of(Verdict.ACCEPTED, Verdict.DUPLICATE, Verdict.ACCEPTED),
        first.events().stream().map(EventResult::verdict).toList());
    assertTrue(retry.allDuplicates());
    assertEquals(2, stored.size());
  }

  @Test
  void outOfOrderReadingsAreDroppedButOtherTypesFlow() {
    clock.set(T0.plus(Duration.ofMinutes(10)));
    send(List.of(simulator.temperature("4", T0.plus(Duration.ofMinutes(5)))));

    IngestionReport late =
        send(List.of(simulator.temperature("9", T0), simulator.door(true, T0.plusSeconds(1))));

    assertEquals(
        List.of(Verdict.DROPPED, Verdict.ACCEPTED),
        late.events().stream().map(EventResult::verdict).toList());
  }

  @Test
  void productRemovalIsSeenOnlyThroughStableReadings() {
    clock.set(T0.plusSeconds(10));

    IngestionReport report = send(simulator.productRemoval("842", "650", T0));

    assertEquals(1, report.count(Verdict.DROPPED));
    assertEquals(FindingKind.STOCK_DECREASE, report.findings().getFirst().kind());
    assertEquals(List.of(AlertPattern.WEIGHT_CONFIRMED_BLINK), buzzer());
  }

  @Test
  void enforcesTheBatchLimit() {
    clock.set(T0.plus(Duration.ofHours(1)));
    List<Esp32Envelope> many = new ArrayList<>();
    for (int index = 0; index < 501; index++) {
      many.add(simulator.temperature("4", T0.plusSeconds(index)));
    }

    assertThrows(MalformedSensorPayloadException.class, () -> send(many));
    assertEquals(500, send(many.subList(0, 500)).count(Verdict.ACCEPTED));
  }

  @Test
  void simulatedDevicesMayOmitIdentityAndTime() {
    Device simulated =
        Device.register(
            HouseholdId.newId(), FridgeId.newId(), "Sim", DeviceKind.SIMULATOR, "cd", T0);

    IngestionReport report =
        ingest.ingest(
            simulated,
            "[{\"type\":\"DOOR\",\"door\":\"OPEN\"},{\"type\":\"TEMPERATURE\",\"tempC\":3.5}]");

    assertEquals(2, report.count(Verdict.ACCEPTED));
    assertThrows(
        MalformedSensorPayloadException.class,
        () -> ingest.ingest(device, "[{\"type\":\"DOOR\",\"door\":\"OPEN\"}]"));
  }
}
