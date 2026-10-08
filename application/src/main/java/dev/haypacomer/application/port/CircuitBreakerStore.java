package dev.haypacomer.application.port;

import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.CircuitState;

public interface CircuitBreakerStore {

  CircuitState load(AdvisorSource provider);

  void save(AdvisorSource provider, CircuitState state);
}
