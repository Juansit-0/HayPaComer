package dev.haypacomer.domain.quantity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Grams(BigDecimal value) implements Comparable<Grams> {

  private static final int SCALE = 2;

  public static final Grams ZERO = new Grams(BigDecimal.ZERO);

  public Grams {
    Objects.requireNonNull(value, "value");
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Grams cannot be negative: " + value);
    }
    value = value.setScale(SCALE, RoundingMode.HALF_UP);
  }

  public static Grams of(long grams) {
    return new Grams(BigDecimal.valueOf(grams));
  }

  public static Grams of(String grams) {
    return new Grams(new BigDecimal(grams));
  }

  public static Grams of(BigDecimal grams) {
    return new Grams(grams);
  }

  public Grams plus(Grams other) {
    return new Grams(value.add(other.value));
  }

  public Grams minus(Grams other) {
    if (other.value.compareTo(value) > 0) {
      throw new IllegalArgumentException("Cannot subtract " + other + " from " + this);
    }
    return new Grams(value.subtract(other.value));
  }

  public Grams times(BigDecimal factor) {
    return new Grams(value.multiply(factor));
  }

  public Grams shortfallTo(Grams required) {
    return isAtLeast(required) ? ZERO : required.minus(this);
  }

  public boolean isAtLeast(Grams other) {
    return value.compareTo(other.value) >= 0;
  }

  public boolean isZero() {
    return value.signum() == 0;
  }

  @Override
  public int compareTo(Grams other) {
    return value.compareTo(other.value);
  }

  @Override
  public String toString() {
    return value.stripTrailingZeros().toPlainString() + " g";
  }
}
