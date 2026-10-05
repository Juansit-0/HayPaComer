package dev.haypacomer.domain.scale;

import java.time.Instant;
import java.util.Objects;

public record RawSample(long counts, Instant at) {

  public RawSample {
    Objects.requireNonNull(at, "at");
  }
}
