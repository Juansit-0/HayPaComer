package dev.haypacomer.application.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CircuitStateTest {

  private static final Instant T0 = Instant.parse("2026-10-08T20:00:00Z");
  private static final CircuitPolicy POLICY = new CircuitPolicy(3, Duration.ofSeconds(60));

  @Test
  void opensAfterTheThresholdAndRetriesOnceAfterTheCooldown() {
    CircuitState state = CircuitState.CLOSED.failure(T0, POLICY).failure(T0, POLICY);
    assertEquals(CircuitPhase.CLOSED, state.phase());
    assertEquals(2, state.failures());

    CircuitState open = state.failure(T0, POLICY);
    assertEquals(CircuitPhase.OPEN, open.phase());
    assertFalse(open.allows(T0.plusSeconds(59), POLICY));
    assertSame(open, open.attempt(T0.plusSeconds(59), POLICY));

    CircuitState trial = open.attempt(T0.plusSeconds(60), POLICY);
    assertEquals(CircuitPhase.HALF_OPEN, trial.phase());
    assertTrue(trial.allows(T0.plusSeconds(60), POLICY));
    assertEquals(CircuitPhase.OPEN, trial.failure(T0.plusSeconds(61), POLICY).phase());
    assertEquals(T0.plusSeconds(61), trial.failure(T0.plusSeconds(61), POLICY).openedAt());
    assertEquals(CircuitState.CLOSED, trial.success());
    assertSame(CircuitState.CLOSED, CircuitState.CLOSED.attempt(T0, POLICY));
  }

  @Test
  void rejectsInvalidStatesAndPolicies() {
    assertThrows(
        IllegalArgumentException.class, () -> new CircuitState(CircuitPhase.OPEN, 3, null));
    assertThrows(
        IllegalArgumentException.class, () -> new CircuitState(CircuitPhase.CLOSED, -1, null));
    assertThrows(IllegalArgumentException.class, () -> new CircuitPolicy(0, Duration.ofSeconds(1)));
    assertThrows(IllegalArgumentException.class, () -> new CircuitPolicy(1, Duration.ZERO));
    assertEquals(3, CircuitPolicy.DEFAULT.failureThreshold());
  }
}
