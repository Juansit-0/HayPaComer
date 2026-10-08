package dev.haypacomer.domain.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StepTimerTest {

  private static final Instant T0 = Instant.parse("2026-10-08T18:00:00Z");
  private static final CookingSessionId SESSION = CookingSessionId.newId();

  @Test
  void countsDownAndPausesWithoutLosingTime() {
    StepTimer timer = StepTimer.start(SESSION, 2, Duration.ofMinutes(15), T0);

    assertEquals(Duration.ofMinutes(10), timer.remaining(T0.plusSeconds(300)));
    StepTimer paused = timer.pause(T0.plusSeconds(300));
    assertTrue(paused.paused());
    assertSame(paused, paused.pause(T0.plusSeconds(400)));
    assertEquals(Duration.ofMinutes(10), paused.remaining(T0.plusSeconds(3_000)));
    assertFalse(paused.due(T0.plusSeconds(3_000)));

    StepTimer resumed = paused.resume(T0.plusSeconds(1_000));
    assertSame(resumed, resumed.resume(T0.plusSeconds(1_001)));
    assertEquals(Duration.ofMinutes(5), resumed.remaining(T0.plusSeconds(1_300)));
    assertFalse(resumed.due(T0.plusSeconds(1_599)));
    assertTrue(resumed.due(T0.plusSeconds(1_600)));
    assertEquals(Duration.ZERO, resumed.remaining(T0.plusSeconds(9_000)));
    assertTrue(resumed.markSignaled().signaled());
    assertEquals(Duration.ZERO, timer.elapsed(T0.minusSeconds(5)));
  }

  @Test
  void rejectsInvalidTimers() {
    assertThrows(
        IllegalArgumentException.class,
        () -> StepTimer.start(SESSION, 0, Duration.ofSeconds(1), T0));
    assertThrows(
        IllegalArgumentException.class, () -> StepTimer.start(SESSION, 1, Duration.ZERO, T0));
  }
}
