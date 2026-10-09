package dev.haypacomer.application.coldchain;

import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.coldchain.ColdChainPhase;
import dev.haypacomer.domain.device.Device;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import dev.haypacomer.domain.sensor.TemperatureReading;
import java.util.Objects;

public final class TrackColdChain {

  private final ColdChainRepository chains;
  private final PolicySource policies;

  public TrackColdChain(ColdChainRepository chains, FridgeThresholds thresholds) {
    this(chains, FixedPolicies.DEFAULT.withThresholds(thresholds));
  }

  public TrackColdChain(ColdChainRepository chains, PolicySource policies) {
    this.chains = Objects.requireNonNull(chains, "chains");
    this.policies = Objects.requireNonNull(policies, "policies");
  }

  public ColdChainPhase record(Device device, TemperatureReading reading) {
    if (!reading.fridge().equals(device.fridge())) {
      throw new IllegalArgumentException("Reading does not belong to this device");
    }
    synchronized (this) {
      ColdChain chain =
          chains.find(device.fridge()).orElseGet(() -> ColdChain.start(device.fridge()));
      ColdChainPhase phase =
          chain.record(
              reading.celsius(), reading.occurredAt(), policies.thresholdsOf(device.fridge()));
      chains.save(chain);
      return phase;
    }
  }
}
