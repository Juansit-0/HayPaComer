package dev.haypacomer.domain.analytics;

import java.time.LocalDate;
import java.util.Objects;

public record DayTally(LocalDate day, Tally tally) {

  public DayTally {
    Objects.requireNonNull(day, "day");
    Objects.requireNonNull(tally, "tally");
  }
}
