package dev.haypacomer.domain.sensor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public abstract class MeasurementChannel {

  private static final Duration RETENTION = Duration.ofHours(6);

  private final List<Measurement> history = new ArrayList<>();

  public final boolean record(SensorEvent event) {
    Optional<Measurement> measurement = measure(event);
    measurement.ifPresent(
        value -> {
          history.add(value);
          history.sort(Comparator.comparing(Measurement::at));
          Instant cutoff = history.getLast().at().minus(RETENTION);
          history.removeIf(old -> old.at().isBefore(cutoff));
        });
    return measurement.isPresent();
  }

  public final List<Measurement> history() {
    return List.copyOf(history);
  }

  public final Optional<Measurement> latest() {
    return history.isEmpty() ? Optional.empty() : Optional.of(history.getLast());
  }

  public abstract String name();

  protected abstract Optional<Measurement> measure(SensorEvent event);
}
