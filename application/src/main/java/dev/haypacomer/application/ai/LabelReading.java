package dev.haypacomer.application.ai;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record LabelReading(
    String product, LocalDate printedDate, double confidence, AdvisorSource source) {

  public LabelReading {
    Objects.requireNonNull(source, "source");
    product = product == null ? "" : product.strip();
    if (confidence < 0 || confidence > 1) {
      throw new IllegalArgumentException("Confidence must be between 0 and 1: " + confidence);
    }
  }

  public Optional<LocalDate> date() {
    return Optional.ofNullable(printedDate);
  }
}
