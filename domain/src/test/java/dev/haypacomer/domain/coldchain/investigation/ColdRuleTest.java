package dev.haypacomer.domain.coldchain.investigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ColdRuleTest {

  @Test
  void defaultIsTheTwoHourRule() {
    assertEquals(Duration.ofHours(2), ColdRule.DEFAULT.discardAfter());
    assertEquals(Duration.ofMinutes(30), ColdRule.DEFAULT.useTodayAfter());
  }

  @Test
  void useTodayComesBeforeDiscard() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ColdRule(Duration.ofMinutes(30), Duration.ofMinutes(45)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ColdRule(Duration.ofMinutes(30), Duration.ofMinutes(-1)));
    assertThrows(NullPointerException.class, () -> new ColdRule(null, Duration.ZERO));
  }
}
