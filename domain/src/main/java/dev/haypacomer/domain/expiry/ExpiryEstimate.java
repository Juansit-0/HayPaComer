package dev.haypacomer.domain.expiry;

import java.time.LocalDate;
import java.util.Objects;

public record ExpiryEstimate(LocalDate date, ExpirySource source, int shelfDays) {

  public ExpiryEstimate {
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(source, "source");
  }

  public double confidence() {
    return source.confidence();
  }
}
