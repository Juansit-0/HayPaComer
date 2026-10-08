package dev.haypacomer.sensors.fridge;

import dev.haypacomer.application.port.FridgeSessionRegistry;
import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.FridgeSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryFridgeSessionRegistry implements FridgeSessionRegistry {

  private final Map<FridgeId, FridgeSession> sessions = new ConcurrentHashMap<>();

  @Override
  public FridgeSession sessionOf(FridgeId fridge) {
    return sessions.computeIfAbsent(fridge, FridgeSession::new);
  }
}
