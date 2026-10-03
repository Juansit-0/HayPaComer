package dev.haypacomer.application.port;

import dev.haypacomer.domain.coldchain.ColdChain;
import dev.haypacomer.domain.fridge.FridgeId;
import java.util.Optional;

public interface ColdChainRepository {

  Optional<ColdChain> find(FridgeId fridge);

  void save(ColdChain chain);
}
