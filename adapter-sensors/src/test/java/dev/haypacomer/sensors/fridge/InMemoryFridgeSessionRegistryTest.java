package dev.haypacomer.sensors.fridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.fridge.FridgeSession;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class InMemoryFridgeSessionRegistryTest {

  @Test
  void everyFridgeHasExactlyOneSessionEvenUnderConcurrency() {
    InMemoryFridgeSessionRegistry registry = new InMemoryFridgeSessionRegistry();
    FridgeId kitchen = FridgeId.newId();
    Set<FridgeSession> seen = ConcurrentHashMap.newKeySet();

    IntStream.range(0, 200).parallel().forEach(index -> seen.add(registry.sessionOf(kitchen)));

    assertEquals(1, seen.size());
    assertSame(registry.sessionOf(kitchen), seen.iterator().next());
    assertNotSame(registry.sessionOf(kitchen), registry.sessionOf(FridgeId.newId()));
    assertEquals(kitchen, registry.sessionOf(kitchen).fridge());
  }
}
