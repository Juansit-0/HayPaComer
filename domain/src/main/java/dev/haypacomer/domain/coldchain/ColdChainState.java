package dev.haypacomer.domain.coldchain;

import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public sealed interface ColdChainState permits Normal, Warming, UnderReview {

  ColdChainPhase phase();

  ColdChainState onTemperature(BigDecimal celsius, Instant at, FridgeThresholds thresholds);

  ColdChainState review(UserId reviewer, Instant at, ColdChain context);

  Optional<Instant> since();

  Optional<BigDecimal> peak();

  boolean recovered();
}
