package dev.haypacomer.domain.quantity;

import java.math.BigDecimal;
import java.util.Optional;

public record ConversionFactors(BigDecimal gramsPerMilliliter, BigDecimal gramsPerPiece) {

  public static final ConversionFactors MASS_ONLY = new ConversionFactors(null, null);

  public ConversionFactors {
    requirePositiveWhenPresent(gramsPerMilliliter, "gramsPerMilliliter");
    requirePositiveWhenPresent(gramsPerPiece, "gramsPerPiece");
    gramsPerMilliliter = normalized(gramsPerMilliliter);
    gramsPerPiece = normalized(gramsPerPiece);
  }

  public static ConversionFactors withDensity(String gramsPerMilliliter) {
    return new ConversionFactors(new BigDecimal(gramsPerMilliliter), null);
  }

  public static ConversionFactors withPieceWeight(String gramsPerPiece) {
    return new ConversionFactors(null, new BigDecimal(gramsPerPiece));
  }

  public ConversionFactors andPieceWeight(String grams) {
    return new ConversionFactors(gramsPerMilliliter, new BigDecimal(grams));
  }

  public Optional<BigDecimal> density() {
    return Optional.ofNullable(gramsPerMilliliter);
  }

  public Optional<BigDecimal> pieceWeight() {
    return Optional.ofNullable(gramsPerPiece);
  }

  public Grams toGrams(BigDecimal amount, Unit unit) {
    BigDecimal baseAmount = unit.toBase(amount);
    return switch (unit.kind()) {
      case MASS -> Grams.of(baseAmount);
      case VOLUME -> Grams.of(baseAmount.multiply(required(gramsPerMilliliter, "density", unit)));
      case COUNT -> Grams.of(baseAmount.multiply(required(gramsPerPiece, "piece weight", unit)));
    };
  }

  private static BigDecimal required(BigDecimal factor, String name, Unit unit) {
    if (factor == null) {
      throw new UnconvertibleQuantityException(
          "Cannot convert " + unit.symbol() + " to grams without a " + name);
    }
    return factor;
  }

  private static BigDecimal normalized(BigDecimal factor) {
    return factor == null ? null : factor.stripTrailingZeros();
  }

  private static void requirePositiveWhenPresent(BigDecimal factor, String name) {
    if (factor != null && factor.signum() <= 0) {
      throw new IllegalArgumentException(name + " must be positive: " + factor);
    }
  }
}
