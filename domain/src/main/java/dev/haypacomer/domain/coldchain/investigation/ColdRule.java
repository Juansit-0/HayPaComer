package dev.haypacomer.domain.coldchain.investigation;

import java.time.Duration;
import java.util.Objects;

public record ColdRule(Duration discardAfter, Duration useTodayAfter) {

  public static final ColdRule DEFAULT = new ColdRule(Duration.ofHours(2), Duration.ofMinutes(30));

  public ColdRule {
    Objects.requireNonNull(discardAfter, "discardAfter");
    Objects.requireNonNull(useTodayAfter, "useTodayAfter");
    if (useTodayAfter.isNegative() || useTodayAfter.compareTo(discardAfter) >= 0) {
      throw new IllegalArgumentException("Use today must come before discard");
    }
  }
}
