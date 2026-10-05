package dev.haypacomer.domain.scale;

import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record WeighingTarget(String food, Grams target, BigDecimal tolerance) {

  public static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("0.03");

  public WeighingTarget {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(tolerance, "tolerance");
    food = food.strip();
    if (food.isEmpty()) {
      throw new IllegalArgumentException("Weighing target needs a food");
    }
    if (target.isZero()) {
      throw new IllegalArgumentException("Weighing target must be above zero");
    }
    if (tolerance.signum() < 0 || tolerance.compareTo(BigDecimal.ONE) >= 0) {
      throw new IllegalArgumentException("Tolerance must be between 0 and 1");
    }
  }

  public static WeighingTarget of(String food, Grams target) {
    return new WeighingTarget(food, target, DEFAULT_TOLERANCE);
  }

  public WeighingProgress evaluate(Grams measured) {
    BigDecimal margin = target.value().multiply(tolerance);
    BigDecimal difference = measured.value().subtract(target.value());
    WeighingStatus status =
        difference.abs().compareTo(margin) <= 0
            ? WeighingStatus.ON_TARGET
            : difference.signum() < 0 ? WeighingStatus.SHORT : WeighingStatus.OVER;
    int percent =
        measured
            .value()
            .multiply(BigDecimal.valueOf(100))
            .divide(target.value(), 0, RoundingMode.HALF_UP)
            .intValue();
    return new WeighingProgress(
        food, measured, target, measured.shortfallTo(target), percent, status);
  }
}
