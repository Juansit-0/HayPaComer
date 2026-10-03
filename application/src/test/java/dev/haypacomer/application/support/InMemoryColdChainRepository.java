package dev.haypacomer.application.support;

import dev.haypacomer.application.port.ColdChainRepository;
import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.fridge.FridgeId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryColdChainRepository implements ColdChainRepository {

  private final Map<FridgeId, ColdChain> chains = new HashMap<>();

  @Override
  public Optional<ColdChain> find(FridgeId fridge) {
    return Optional.ofNullable(chains.get(fridge))
        .map(
            chain ->
                ColdChain.restore(
                    chain.fridge(),
                    chain.state(),
                    chain.lastCelsius().orElse(null),
                    chain.lastReadingAt().orElse(null)));
  }

  @Override
  public void save(ColdChain chain) {
    chains.put(chain.fridge(), chain);
  }
}
