package dev.haypacomer.sensors.cooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.session.CookingSessionId;
import dev.haypacomer.domain.session.StepTimer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryStepTimerStoreTest {

  @Test
  void keepsOneTimerPerSession() {
    InMemoryStepTimerStore store = new InMemoryStepTimerStore();
    CookingSessionId session = CookingSessionId.newId();
    StepTimer first = StepTimer.start(session, 1, Duration.ofMinutes(1), Instant.EPOCH);
    StepTimer second = StepTimer.start(session, 2, Duration.ofMinutes(2), Instant.EPOCH);

    store.save(first);
    store.save(second);

    assertEquals(second, store.find(session).orElseThrow());
    assertEquals(List.of(second), store.all());
    store.remove(session);
    assertTrue(store.find(session).isEmpty());
  }
}
