package dev.haypacomer.domain.sensor;

import dev.haypacomer.domain.fridge.FridgeId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FridgeMonitor {

  private final FridgeId fridge;
  private final List<MeasurementChannel> channels;
  private final List<Interpretation> interpretations;

  public FridgeMonitor(FridgeId fridge, FridgeThresholds thresholds) {
    this.fridge = Objects.requireNonNull(fridge, "fridge");
    DoorChannel door = new DoorChannel();
    TemperatureChannel temperature = new TemperatureChannel();
    WeightChannel stock = new WeightChannel(ScaleMode.FRIDGE);
    this.channels = List.of(door, temperature, stock);
    this.interpretations =
        List.of(
            new SustainedThreshold(
                door,
                new BigDecimal("0.5"),
                thresholds.doorAlertAfter(),
                FindingKind.DOOR_LEFT_OPEN),
            new SustainedThreshold(
                temperature,
                thresholds.maxCelsius(),
                thresholds.coldChainGrace(),
                FindingKind.COLD_CHAIN_BREACH),
            new StableDelta(stock, thresholds.minimumWeightChange()));
  }

  public FridgeId fridge() {
    return fridge;
  }

  public Optional<Measurement> latest(String channel) {
    return channels.stream()
        .filter(candidate -> candidate.name().equals(channel))
        .findFirst()
        .flatMap(MeasurementChannel::latest);
  }

  public boolean record(SensorEvent event) {
    if (!event.fridge().equals(fridge)) {
      throw new IllegalArgumentException("Event belongs to another fridge");
    }
    return channels.stream()
        .map(channel -> channel.record(event))
        .reduce(false, Boolean::logicalOr);
  }

  public List<Finding> evaluate(Instant now) {
    return interpretations.stream()
        .map(interpretation -> interpretation.evaluate(now))
        .flatMap(Optional::stream)
        .toList();
  }
}
