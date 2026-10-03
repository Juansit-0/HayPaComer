package dev.haypacomer.domain.coldchain;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ColdChain {

  private final FridgeId fridge;
  private final List<ColdIncident> closedIncidents = new ArrayList<>();
  private ColdChainState state;
  private BigDecimal lastCelsius;
  private Instant lastReadingAt;

  private ColdChain(FridgeId fridge, ColdChainState state) {
    this.fridge = Objects.requireNonNull(fridge, "fridge");
    this.state = Objects.requireNonNull(state, "state");
  }

  public static ColdChain start(FridgeId fridge) {
    return new ColdChain(fridge, new Normal());
  }

  public static ColdChain restore(
      FridgeId fridge, ColdChainState state, BigDecimal lastCelsius, Instant lastReadingAt) {
    ColdChain chain = new ColdChain(fridge, state);
    chain.lastCelsius = lastCelsius;
    chain.lastReadingAt = lastReadingAt;
    return chain;
  }

  public FridgeId fridge() {
    return fridge;
  }

  public ColdChainState state() {
    return state;
  }

  public ColdChainPhase phase() {
    return state.phase();
  }

  public Optional<BigDecimal> lastCelsius() {
    return Optional.ofNullable(lastCelsius);
  }

  public Optional<Instant> lastReadingAt() {
    return Optional.ofNullable(lastReadingAt);
  }

  public boolean needsReview() {
    return state.phase() == ColdChainPhase.UNDER_REVIEW;
  }

  public ColdChainPhase record(BigDecimal celsius, Instant at, FridgeThresholds thresholds) {
    Objects.requireNonNull(celsius, "celsius");
    Objects.requireNonNull(at, "at");
    if (lastReadingAt != null && at.isBefore(lastReadingAt)) {
      return state.phase();
    }
    state = state.onTemperature(celsius, at, thresholds);
    lastCelsius = celsius;
    lastReadingAt = at;
    return state.phase();
  }

  public ColdIncident review(UserId reviewer, Instant at) {
    state = state.review(Objects.requireNonNull(reviewer, "reviewer"), at, this);
    return closedIncidents.getLast();
  }

  public List<ColdIncident> closedIncidents() {
    return List.copyOf(closedIncidents);
  }

  void close(ColdIncident incident) {
    closedIncidents.add(incident);
  }
}
