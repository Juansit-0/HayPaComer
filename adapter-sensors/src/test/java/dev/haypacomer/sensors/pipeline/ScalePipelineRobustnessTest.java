package dev.haypacomer.sensors.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.application.port.DeviceRepository;
import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.application.sensor.HardwareFactories;
import dev.haypacomer.application.sensor.IngestSensorEvents;
import dev.haypacomer.application.sensor.IngestionReport;
import dev.haypacomer.application.sensor.ObserveSensorEvent;
import dev.haypacomer.application.sensor.validation.SensorEventTypes;
import dev.haypacomer.application.sensor.validation.ValidateSensorEvent;
import dev.haypacomer.application.sensor.validation.Verdict;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.device.DeviceId;
import dev.haypacomer.domain.device.DeviceKind;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.scale.RawSample;
import dev.haypacomer.domain.scale.ScaleCalibration;
import dev.haypacomer.domain.scale.WeighingTarget;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.ScaleMode;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.SensorEventId;
import dev.haypacomer.domain.sensor.WeightReading;
import dev.haypacomer.sensors.esp32.Esp32Envelope;
import dev.haypacomer.sensors.esp32.Esp32Simulator;
import dev.haypacomer.sensors.hardware.Esp32HardwareFactory;
import dev.haypacomer.sensors.hardware.SimulatedHardwareFactory;
import dev.haypacomer.sensors.monitor.InMemoryFridgeMonitorRegistry;
import dev.haypacomer.sensors.scale.InMemoryCalibrations;
import dev.haypacomer.sensors.scale.InMemoryScaleSampleStore;
import dev.haypacomer.sensors.scale.InMemoryScaleSessionStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScalePipelineRobustnessTest {

  private static final Instant T0 = Instant.parse("2026-10-05T18:00:00Z");
  private static final long OFFSET = 84_000;
  private static final long PER_GRAM = 428;

  private final Esp32Simulator simulator = new Esp32Simulator("scale-01");
  private final InMemoryCalibrations calibrations = new InMemoryCalibrations();
  private final InMemoryScaleSampleStore samples = new InMemoryScaleSampleStore();
  private final InMemoryScaleSessionStore sessions = new InMemoryScaleSessionStore();
  private final Map<SensorEventId, SensorEvent> stored = new HashMap<>();
  private final List<WeightReading> handled = new ArrayList<>();
  private Device scale;
  private IngestSensorEvents ingest;

  @BeforeEach
  void wire() {
    scale =
        Device.register(
            HouseholdId.newId(), FridgeId.newId(), "Scale", DeviceKind.ESP32_SCALE, "ab", T0);
    Clock clock = Clock.fixed(T0.plusSeconds(60), ZoneOffset.UTC);
    HardwareFactories hardware =
        new HardwareFactories(
            List.of(
                new Esp32HardwareFactory(calibrations, samples, sessions),
                new SimulatedHardwareFactory(clock, calibrations, samples, sessions)));
    SensorEventLog log =
        new SensorEventLog() {
          @Override
          public boolean contains(SensorEventId id) {
            return stored.containsKey(id);
          }

          @Override
          public Optional<Instant> lastAccepted(DeviceId device, String type) {
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
    ColdChainRepository chains =
        new ColdChainRepository() {
          @Override
          public Optional<ColdChain> find(FridgeId fridge) {
            return Optional.empty();
          }

          @Override
          public void save(ColdChain chain) {}
        };
    DeviceRepository devices =
        new DeviceRepository() {
          @Override
          public void save(Device device) {}

          @Override
          public Optional<Device> findById(DeviceId id) {
            return Optional.of(scale);
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
    ingest =
        new IngestSensorEvents(
            hardware,
            new ValidateSensorEvent(log, clock),
            log,
            new ObserveSensorEvent(
                new InMemoryFridgeMonitorRegistry(FridgeThresholds.DEFAULT), devices, hardware),
            new TrackColdChain(chains, FridgeThresholds.DEFAULT),
            (device, reading) -> {
              handled.add(reading);
              return Optional.empty();
            },
            clock);
  }

  private void calibrate() {
    calibrations.save(
        scale.id(),
        ScaleCalibration.taredAt(new RawSample(OFFSET, T0))
            .calibrate(new RawSample(OFFSET + PER_GRAM * 500, T0), Grams.of(500)));
  }

  private IngestionReport raw(List<Esp32Envelope> envelopes) {
    return ingest.ingest(scale, simulator.toJson(envelopes));
  }

  private Esp32Envelope grams(long grams, long millis) {
    return simulator.rawWeight(OFFSET + PER_GRAM * grams, null, T0.plusMillis(millis));
  }

  @Test
  void uncalibratedSamplesOnlyFeedTareAndCalibration() {
    IngestionReport report = raw(List.of(grams(842, 0), grams(842, 500)));

    assertTrue(report.events().isEmpty());
    assertTrue(handled.isEmpty());
    assertEquals(OFFSET + PER_GRAM * 842, samples.latest(scale.id()).orElseThrow().counts());
  }

  @Test
  void onlyStableReadingsReachTheStockHandler() {
    calibrate();

    IngestionReport report =
        raw(
            List.of(
                grams(842, 0),
                grams(842, 1_000),
                grams(700, 1_200),
                grams(651, 1_500),
                grams(650, 2_000),
                grams(650, 2_600)));

    assertEquals(
        List.of(
            Verdict.DROPPED,
            Verdict.ACCEPTED,
            Verdict.DROPPED,
            Verdict.DROPPED,
            Verdict.DROPPED,
            Verdict.ACCEPTED),
        report.events().stream().map(event -> event.verdict()).toList());
    assertEquals(
        List.of(Grams.of(842), Grams.of(650)), handled.stream().map(WeightReading::grams).toList());
  }

  @Test
  void repeatedHeartbeatsAreDuplicatesOrStale() {
    calibrate();
    List<Esp32Envelope> stable = List.of(grams(650, 0), grams(650, 1_100));
    raw(stable);

    IngestionReport retry = raw(stable);
    IngestionReport old =
        raw(List.of(simulator.rawWeight(OFFSET + PER_GRAM * 650, null, T0.minusSeconds(30))));

    assertEquals(
        List.of(Verdict.DROPPED, Verdict.DUPLICATE),
        retry.events().stream().map(event -> event.verdict()).toList());
    assertEquals(Verdict.DROPPED, old.events().getFirst().verdict());
    assertEquals(1, handled.size());
  }

  @Test
  void cookingModeReadingsCarryTheModeAndTheTargetCanBeEvaluated() {
    calibrate();
    sessions.cook(scale.id(), WeighingTarget.of("Chicken breast", Grams.of(200)));

    raw(List.of(grams(80, 0), grams(80, 1_100)));

    assertEquals(ScaleMode.COOKING, handled.getLast().mode());
    assertEquals(
        Grams.of(120),
        sessions.target(scale.id()).orElseThrow().evaluate(handled.getLast().grams()).remaining());
  }
}
