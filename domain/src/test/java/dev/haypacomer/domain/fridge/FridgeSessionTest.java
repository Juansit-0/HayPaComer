package dev.haypacomer.domain.fridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FridgeSessionTest {

  private static final Instant T0 = Instant.parse("2026-10-08T20:00:00Z");

  @Test
  void onePersonAtATimeUntilTheyLeaveOrGoIdle() {
    FridgeSession session = new FridgeSession(FridgeId.newId());
    UserId juan = UserId.newId();
    UserId ana = UserId.newId();

    assertTrue(session.activeUser(T0).isEmpty());
    session.claim(juan, T0);
    assertEquals(juan, session.activeUser(T0.plusSeconds(60)).orElseThrow());
    assertEquals(T0.plus(FridgeSession.IDLE_TIMEOUT), session.expiresAt(T0).orElseThrow());
    assertThrows(FridgeBusyException.class, () -> session.claim(ana, T0.plusSeconds(60)));

    session.claim(juan, T0.plusSeconds(120));
    assertEquals(
        T0.plusSeconds(120).plus(FridgeSession.IDLE_TIMEOUT),
        session.expiresAt(T0.plusSeconds(120)).orElseThrow());
    session.release(ana, T0.plusSeconds(130));
    assertEquals(juan, session.activeUser(T0.plusSeconds(130)).orElseThrow());
    session.release(juan, T0.plusSeconds(140));
    assertTrue(session.activeUser(T0.plusSeconds(140)).isEmpty());

    session.claim(ana, T0.plusSeconds(150));
    Instant idle = T0.plusSeconds(150).plus(FridgeSession.IDLE_TIMEOUT);
    assertTrue(session.activeUser(idle).isEmpty());
    assertTrue(session.expiresAt(idle).isEmpty());
    session.claim(juan, idle);
    assertEquals(juan, session.activeUser(idle).orElseThrow());
  }
}
