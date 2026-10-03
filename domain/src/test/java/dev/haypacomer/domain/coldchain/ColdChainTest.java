package dev.haypacomer.domain.coldchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.fridge.FridgeId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.sensor.FridgeThresholds;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ColdChainTest {

  private static final Instant T0 = Instant.parse("2026-10-03T18:00:00Z");
  private static final FridgeThresholds LIMITS = FridgeThresholds.DEFAULT;

  private final ColdChain chain = ColdChain.start(FridgeId.newId());
  private final UserId juan = UserId.newId();

  private ColdChainPhase at(String celsius, long minutes) {
    return chain.record(new BigDecimal(celsius), T0.plus(Duration.ofMinutes(minutes)), LIMITS);
  }

  @Test
  void shortWarmingReturnsToNormalWithoutReview() {
    assertEquals(ColdChainPhase.NORMAL, at("4", 0));
    assertEquals(ColdChainPhase.WARMING, at("7", 5));
    assertEquals(ColdChainPhase.WARMING, at("8", 15));
    assertEquals(ColdChainPhase.NORMAL, at("4.5", 18));
    assertFalse(chain.needsReview());
    assertThrows(IllegalStateException.class, () -> chain.review(juan, T0));
  }

  @Test
  void sustainedWarmingNeedsAHumanReviewAfterRecovery() {
    at("7", 0);
    assertThrows(IllegalStateException.class, () -> chain.review(juan, T0));
    assertEquals(ColdChainPhase.UNDER_REVIEW, at("9.5", 20));
    assertTrue(chain.needsReview());
    assertEquals(new BigDecimal("9.5"), chain.state().peak().orElseThrow());

    assertThrows(
        IllegalStateException.class, () -> chain.review(juan, T0.plus(Duration.ofMinutes(21))));
    assertEquals(ColdChainPhase.UNDER_REVIEW, at("4", 40));
    assertTrue(chain.state().recovered());
    assertEquals(ColdChainPhase.UNDER_REVIEW, at("6", 45));
    assertFalse(chain.state().recovered());
    at("3.5", 60);

    ColdIncident incident = chain.review(juan, T0.plus(Duration.ofMinutes(62)));

    assertEquals(ColdChainPhase.NORMAL, chain.phase());
    assertEquals(T0, incident.startedAt());
    assertEquals(new BigDecimal("9.5"), incident.peakCelsius());
    assertEquals(juan, incident.reviewedBy());
    assertEquals(1, chain.closedIncidents().size());
  }

  @Test
  void ignoresOutOfOrderReadingsAndRestoresState() {
    at("7", 10);
    assertEquals(ColdChainPhase.WARMING, at("3", 5));
    assertEquals(new BigDecimal("7"), chain.lastCelsius().orElseThrow());

    ColdChain restored =
        ColdChain.restore(
            chain.fridge(),
            chain.state(),
            chain.lastCelsius().orElseThrow(),
            chain.lastReadingAt().orElseThrow());

    assertInstanceOf(Warming.class, restored.state());
    assertEquals(T0.plus(Duration.ofMinutes(10)), restored.state().since().orElseThrow());
    assertTrue(new Normal().since().isEmpty());
    assertTrue(new Normal().peak().isEmpty());
  }
}
