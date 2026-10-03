package dev.haypacomer.domain.coldchain;

import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public record Normal() implements ColdChainState {

  @Override
  public ColdChainPhase phase() {
    return ColdChainPhase.NORMAL;
  }

  @Override
  public ColdChainState onTemperature(BigDecimal celsius, Instant at, FridgeThresholds thresholds) {
    return celsius.compareTo(thresholds.maxCelsius()) > 0 ? new Warming(at, celsius) : this;
  }

  @Override
  public ColdChainState review(UserId reviewer, Instant at, ColdChain context) {
    throw new IllegalStateException("The cold chain has nothing to review");
  }

  @Override
  public Optional<Instant> since() {
    return Optional.empty();
  }

  @Override
  public Optional<BigDecimal> peak() {
    return Optional.empty();
  }

  @Override
  public boolean recovered() {
    return true;
  }
}
