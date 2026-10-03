package dev.haypacomer.domain.sensor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SustainedThreshold extends Interpretation {

  private final BigDecimal threshold;
  private final Duration minimum;
  private final FindingKind kind;

  public SustainedThreshold(
      MeasurementChannel channel, BigDecimal threshold, Duration minimum, FindingKind kind) {
    super(channel);
    this.threshold = Objects.requireNonNull(threshold, "threshold");
    this.minimum = Objects.requireNonNull(minimum, "minimum");
    this.kind = Objects.requireNonNull(kind, "kind");
  }

  @Override
  public Optional<Finding> evaluate(Instant now) {
    List<Measurement> history = channel.history();
    if (history.isEmpty() || !above(history.getLast())) {
      return Optional.empty();
    }
    int start = history.size() - 1;
    BigDecimal peak = history.getLast().value();
    while (start > 0 && above(history.get(start - 1))) {
      start--;
      peak = peak.max(history.get(start).value());
    }
    Instant since = history.get(start).at();
    Duration duration = Duration.between(since, now);
    if (duration.compareTo(minimum) < 0) {
      return Optional.empty();
    }
    return Optional.of(new Finding(kind, channel.name(), since, duration, peak));
  }

  private boolean above(Measurement measurement) {
    return measurement.value().compareTo(threshold) > 0;
  }
}
