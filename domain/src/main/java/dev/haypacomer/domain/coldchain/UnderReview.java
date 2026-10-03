package dev.haypacomer.domain.coldchain;

import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record UnderReview(Instant startedAt, BigDecimal highest, boolean isRecovered)
    implements ColdChainState {

  public UnderReview {
    Objects.requireNonNull(startedAt, "startedAt");
    Objects.requireNonNull(highest, "highest");
  }

  @Override
  public ColdChainPhase phase() {
    return ColdChainPhase.UNDER_REVIEW;
  }

  @Override
  public ColdChainState onTemperature(BigDecimal celsius, Instant at, FridgeThresholds thresholds) {
    return new UnderReview(
        startedAt, highest.max(celsius), celsius.compareTo(thresholds.maxCelsius()) <= 0);
  }

  @Override
  public ColdChainState review(UserId reviewer, Instant at, ColdChain context) {
    if (!isRecovered) {
      throw new IllegalStateException("The temperature is still out of range");
    }
    context.close(new ColdIncident(startedAt, at, highest, reviewer));
    return new Normal();
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
    return isRecovered;
  }
}
