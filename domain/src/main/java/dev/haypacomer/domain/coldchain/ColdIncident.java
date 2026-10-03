package dev.haypacomer.domain.coldchain;

import dev.haypacomer.domain.identity.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record ColdIncident(
    Instant startedAt, Instant reviewedAt, BigDecimal peakCelsius, UserId reviewedBy) {

  public ColdIncident {
    Objects.requireNonNull(startedAt, "startedAt");
    Objects.requireNonNull(reviewedAt, "reviewedAt");
    Objects.requireNonNull(peakCelsius, "peakCelsius");
    Objects.requireNonNull(reviewedBy, "reviewedBy");
  }
}
