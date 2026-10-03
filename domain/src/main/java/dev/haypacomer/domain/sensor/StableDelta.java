package dev.haypacomer.domain.sensor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StableDelta extends Interpretation {

  private final BigDecimal minimumChange;

  public StableDelta(MeasurementChannel channel, BigDecimal minimumChange) {
    super(channel);
    this.minimumChange = Objects.requireNonNull(minimumChange, "minimumChange");
  }

  @Override
  public Optional<Finding> evaluate(Instant now) {
    List<Measurement> stable = channel.history().stream().filter(Measurement::reliable).toList();
    if (stable.size() < 2) {
      return Optional.empty();
    }
    Measurement before = stable.get(stable.size() - 2);
    Measurement after = stable.getLast();
    BigDecimal change = after.value().subtract(before.value());
    if (change.abs().compareTo(minimumChange) < 0) {
      return Optional.empty();
    }
    FindingKind kind =
        change.signum() < 0 ? FindingKind.STOCK_DECREASE : FindingKind.STOCK_INCREASE;
    return Optional.of(
        new Finding(
            kind,
            channel.name(),
            before.at(),
            Duration.between(before.at(), after.at()),
            change.abs()));
  }
}
