package dev.haypacomer.domain.coldchain.investigation;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ColdEpisode(
    Instant start,
    Instant end,
    BigDecimal peakCelsius,
    Duration aboveLimit,
    Duration doorOpen,
    LikelyCause cause) {

  public ColdEpisode {
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(peakCelsius, "peakCelsius");
    Objects.requireNonNull(aboveLimit, "aboveLimit");
    Objects.requireNonNull(doorOpen, "doorOpen");
    Objects.requireNonNull(cause, "cause");
  }

  public Optional<Instant> ended() {
    return Optional.ofNullable(end);
  }
}
