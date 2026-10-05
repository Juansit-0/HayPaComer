package dev.haypacomer.application.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.application.port.AlertSignal;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.FridgeMonitorRegistry;
import dev.haypacomer.application.port.HardwareFactory;
import dev.haypacomer.application.port.SensorEventDecoder;
import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.application.sensor.validation.SensorEventTypes;
import dev.haypacomer.application.sensor.validation.ValidateSensorEvent;
import dev.haypacomer.application.sensor.validation.Verdict;
import dev.haypacomer.application.support.InMemoryColdChainRepository;
import dev.haypacomer.domain.coldchain.ColdChainPhase;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.DoorState;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.FindingKind;
import dev.haypacomer.domain.sensor.FridgeMonitor;
import dev.haypacomer.domain.sensor.FridgeThresholds;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IngestSensorEventsTest {

  private static final Instant NOW = Instant.parse("2026-10-03T18:00:00Z");

  private final Device device =
      Device.register(
          HouseholdId.newId(), FridgeId.newId(), "Probe", DeviceKind.SIMULATOR, "ab", NOW);
  private final List<SensorEvent> decoded = new ArrayList<>();
  private final Map<SensorEventId, SensorEvent> stored = new HashMap<>();
  private final List<AlertPattern> buzzer = new ArrayList<>();
  private final InMemoryColdChainRepository chains = new InMemoryColdChainRepository();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  private final SensorEventLog log =
      new SensorEventLog() {
        @Override
        public boolean contains(SensorEventId id) {
          return stored.containsKey(id);
        }

        @Override
        public Optional<Instant> lastAccepted(DeviceId target, String type) {
          return stored.values().stream()
              .filter(event -> SensorEventTypes.of(event).equals(type))
              .map(SensorEvent::occurredAt)
              .max(Instant::compareTo);
        }

        @Override
        public void accept(SensorEvent event, Instant receivedAt) {
          stored.put(event.id(), event);
        }
      };

  private IngestSensorEvents ingest() {
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
    SensorEventDecoder decoder = (target, payload) -> List.copyOf(decoded);
    HardwareFactory family =
        new HardwareFactory() {
          @Override
          public boolean supports(DeviceKind kind) {
            return true;
          }

          @Override
          public SensorEventDecoder decoder() {
            return decoder;
          }

          @Override
          public AlertSignal alerts() {
            return signal;
          }
        };
    HardwareFactories hardware = new HardwareFactories(List.of(family));
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
    FridgeMonitorRegistry registry =
        new FridgeMonitorRegistry() {
          final Map<FridgeId, FridgeMonitor> monitors = new HashMap<>();
          final Set<String> reported = new HashSet<>();
          DeviceId last;

          @Override
          public FridgeMonitor monitor(FridgeId fridge) {
            return monitors.computeIfAbsent(
                fridge, id -> new FridgeMonitor(id, FridgeThresholds.DEFAULT));
          }

          @Override
          public void rememberDevice(FridgeId fridge, DeviceId id) {
            last = id;
          }

          @Override
          public Optional<DeviceId> lastDevice(FridgeId fridge) {
            return Optional.ofNullable(last);
          }

          @Override
          public Collection<FridgeId> fridges() {
            return monitors.keySet();
          }

          @Override
          public boolean firstReport(FridgeId fridge, Finding finding) {
            return reported.add(finding.kind() + "|" + finding.since());
          }
        };
    return new IngestSensorEvents(
        hardware,
        new ValidateSensorEvent(log, clock),
        log,
        new ObserveSensorEvent(registry, devices, hardware),
        new TrackColdChain(chains, FridgeThresholds.DEFAULT),
        (scale, reading) -> Optional.empty(),
        clock);
  }

  private SensorEventId id() {
    return new SensorEventId(UUID.randomUUID());
  }

  @Test
  void anEmptyReportIsNeitherRejectedNorDuplicate() {
    IngestionReport empty = new IngestionReport(List.of(), List.of());

    assertFalse(empty.allRejected());
    assertFalse(empty.allDuplicates());
  }

  @Test
  void acceptsValidatesAndFeedsMonitorsAndTheColdChain() {
    TemperatureReading warm =
        new TemperatureReading(
            id(),
            device.id(),
            device.fridge(),
            NOW.minus(Duration.ofMinutes(30)),
            new BigDecimal("9"));
    decoded.add(
        new DoorEvent(id(), device.id(), device.fridge(), NOW.minusSeconds(60), DoorState.OPEN));
    decoded.add(warm);
    decoded.add(
        new TemperatureReading(id(), device.id(), device.fridge(), NOW, new BigDecimal("9.5")));
    decoded.add(
        new TemperatureReading(id(), device.id(), device.fridge(), NOW, new BigDecimal("99")));
    decoded.add(
        new WeightReading(
            id(), device.id(), device.fridge(), NOW, Grams.of(650), false, ScaleMode.FRIDGE, null));

    IngestionReport report = ingest().ingest(device, "ignored");

    assertEquals(
        List.of(
            Verdict.ACCEPTED,
            Verdict.ACCEPTED,
            Verdict.ACCEPTED,
            Verdict.REJECTED,
            Verdict.DROPPED),
        report.events().stream().map(EventResult::verdict).toList());
    assertEquals(3, stored.size());
    assertEquals(ColdChainPhase.UNDER_REVIEW, chains.find(device.fridge()).orElseThrow().phase());
    assertTrue(
        report.findings().stream()
            .anyMatch(finding -> finding.kind() == FindingKind.DOOR_LEFT_OPEN));
    assertTrue(
        report.findings().stream()
            .anyMatch(finding -> finding.kind() == FindingKind.COLD_CHAIN_BREACH));
    assertTrue(buzzer.contains(AlertPattern.DOOR_OPEN_BEEP));
    assertFalse(report.allRejected());

    decoded.clear();
    decoded.add(warm);
    IngestionReport replay = ingest().ingest(device, "ignored");
    assertTrue(replay.allDuplicates());
    assertEquals(1, replay.count(Verdict.DUPLICATE));
  }
}
