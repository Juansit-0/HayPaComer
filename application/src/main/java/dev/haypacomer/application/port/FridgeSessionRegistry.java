package dev.haypacomer.application.port;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.FridgeSession;

public interface FridgeSessionRegistry {

  FridgeSession sessionOf(FridgeId fridge);
}
