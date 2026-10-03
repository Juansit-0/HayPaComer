package dev.haypacomer.domain.coldchain;

import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record Warming(Instant startedAt, BigDecimal highest) implements ColdChainState {

  public Warming {
    Objects.requireNonNull(startedAt, "startedAt");
    Objects.requireNonNull(highest, "highest");
  }

  @Override
  public ColdChainPhase phase() {
    return ColdChainPhase.WARMING;
  }

  @Override
  public ColdChainState onTemperature(BigDecimal celsius, Instant at, FridgeThresholds thresholds) {
    if (celsius.compareTo(thresholds.maxCelsius()) <= 0) {
      return new Normal();
    }
    BigDecimal newPeak = highest.max(celsius);
    if (Duration.between(startedAt, at).compareTo(thresholds.coldChainGrace()) >= 0) {
      return new UnderReview(startedAt, newPeak, false);
    }
    return new Warming(startedAt, newPeak);
  }

  @Override
  public ColdChainState review(UserId reviewer, Instant at, ColdChain context) {
    throw new IllegalStateException("The cold chain has nothing to review yet");
  }

  @Override
  public Optional<Instant> since() {
    return Optional.of(startedAt);
  }

  @Override
  public Optional<BigDecimal> peak() {
    return Optional.of(highest);
  }

  @Override
  public boolean recovered() {
    return false;
  }
}
