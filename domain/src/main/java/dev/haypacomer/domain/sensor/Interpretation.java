package dev.haypacomer.domain.sensor;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public abstract class Interpretation {

  protected final MeasurementChannel channel;

  protected Interpretation(MeasurementChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
  }

  public final MeasurementChannel channel() {
    return channel;
  }

  public abstract Optional<Finding> evaluate(Instant now);
}
