package dev.haypacomer.application.sensor;

import dev.haypacomer.application.coldchain.TrackColdChain;
import dev.haypacomer.application.live.BroadcastLiveUpdate;
import dev.haypacomer.application.live.LiveUpdate;
import dev.haypacomer.application.live.LiveUpdateKind;
import dev.haypacomer.application.port.SensorEventLog;
import dev.haypacomer.application.sensor.validation.SensorEventTypes;
import dev.haypacomer.application.sensor.validation.ValidateSensorEvent;
import dev.haypacomer.application.sensor.validation.ValidationResult;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.DoorEvent;
import dev.haypacomer.domain.sensor.Finding;
import dev.haypacomer.domain.sensor.SensorEvent;
import dev.haypacomer.domain.sensor.TemperatureReading;
import dev.haypacomer.domain.sensor.WeightReading;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class IngestSensorEvents {

  private final HardwareFactories hardware;
  private final ValidateSensorEvent validation;
  private final SensorEventLog log;
  private final ObserveSensorEvent observe;
  private final TrackColdChain coldChain;
  private final WeightReadingHandler fridgeScale;
  private final Clock clock;
  private final BroadcastLiveUpdate live;

  public IngestSensorEvents(
      HardwareFactories hardware,
      ValidateSensorEvent validation,
      SensorEventLog log,
      ObserveSensorEvent observe,
      TrackColdChain coldChain,
      WeightReadingHandler fridgeScale,
      Clock clock) {
    this(
        hardware,
        validation,
        log,
        observe,
        coldChain,
        fridgeScale,
        BroadcastLiveUpdate.NOBODY,
        clock);
  }

  public IngestSensorEvents(
      HardwareFactories hardware,
      ValidateSensorEvent validation,
      SensorEventLog log,
      ObserveSensorEvent observe,
      TrackColdChain coldChain,
      WeightReadingHandler fridgeScale,
      BroadcastLiveUpdate live,
      Clock clock) {
    this.live = Objects.requireNonNull(live, "live");
    this.hardware = Objects.requireNonNull(hardware, "hardware");
    this.validation = Objects.requireNonNull(validation, "validation");
    this.log = Objects.requireNonNull(log, "log");
    this.observe = Objects.requireNonNull(observe, "observe");
    this.coldChain = Objects.requireNonNull(coldChain, "coldChain");
    this.fridgeScale = Objects.requireNonNull(fridgeScale, "fridgeScale");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public IngestionReport ingest(Device device, String payload) {
    List<SensorEvent> events = hardware.forDevice(device).decoder().decode(device, payload);
    List<EventResult> results = new ArrayList<>();
    List<Finding> findings = new ArrayList<>();
    for (SensorEvent event : events) {
      ValidationResult verdict = validation.validate(event);
      results.add(
          new EventResult(
              event.id().value(), SensorEventTypes.of(event), verdict.verdict(), verdict.reason()));
      if (!verdict.accepted()) {
        continue;
      }
      log.accept(event, clock.instant());
      if (event instanceof TemperatureReading reading) {
        coldChain.record(device, reading);
      }
      if (event instanceof WeightReading reading) {
        fridgeScale.apply(device, reading);
      }
      findings.addAll(observe.observe(device, event));
      live(device, event);
    }
    return new IngestionReport(results, findings);
  }

  private void live(Device device, SensorEvent event) {
    String detail =
        switch (event) {
          case DoorEvent door -> "door " + door.state().name().toLowerCase(Locale.ROOT);
          case TemperatureReading reading ->
              reading.celsius().stripTrailingZeros().toPlainString() + " C";
          case WeightReading reading -> reading.grams().value().toPlainString() + " g on the scale";
        };
    live.publish(
        LiveUpdate.of(
            device.household(),
            LiveUpdateKind.SENSOR,
            event.fridge().value(),
            detail,
            event.occurredAt()));
  }
}
