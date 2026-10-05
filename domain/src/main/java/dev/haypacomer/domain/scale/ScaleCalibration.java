package dev.haypacomer.domain.scale;

import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ScaleCalibration(
    long offsetCounts, BigDecimal countsPerGram, Instant taredAt, Instant calibratedAt) {

  public ScaleCalibration {
    Objects.requireNonNull(taredAt, "taredAt");
    if (countsPerGram != null && countsPerGram.signum() == 0) {
      throw new IllegalArgumentException("Calibration factor cannot be zero");
    }
  }

  public static ScaleCalibration taredAt(RawSample empty) {
    return new ScaleCalibration(empty.counts(), null, empty.at(), null);
  }

  public ScaleCalibration tare(RawSample sample) {
    return new ScaleCalibration(sample.counts(), countsPerGram, sample.at(), calibratedAt);
  }

  public ScaleCalibration calibrate(RawSample withKnownWeight, Grams knownWeight) {
    if (knownWeight.isZero()) {
      throw new IllegalArgumentException("Calibration needs a known weight above zero");
    }
    long delta = withKnownWeight.counts() - offsetCounts;
    if (delta == 0) {
      throw new IllegalArgumentException("The scale did not change; place the known weight");
    }
    BigDecimal factor =
        BigDecimal.valueOf(delta).divide(knownWeight.value(), MathContext.DECIMAL64);
    return new ScaleCalibration(offsetCounts, factor, taredAt, withKnownWeight.at());
  }

  public boolean isCalibrated() {
    return countsPerGram != null;
  }

  public Optional<Grams> toGrams(long counts) {
    if (!isCalibrated()) {
      return Optional.empty();
    }
    BigDecimal grams =
        BigDecimal.valueOf(counts - offsetCounts).divide(countsPerGram, MathContext.DECIMAL64);
    return Optional.of(Grams.of(grams.signum() < 0 ? BigDecimal.ZERO : grams));
  }
}
