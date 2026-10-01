package dev.haypacomer.domain.quantity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ConversionFactorsTest {

  @Test
  void convertsVolumeWithDensity() {
    ConversionFactors milk = ConversionFactors.withDensity("1.03");

    assertEquals(Grams.of("206"), milk.toGrams(new BigDecimal("200"), Unit.MILLILITER));
    assertEquals(Grams.of("1030"), milk.toGrams(BigDecimal.ONE, Unit.LITER));
  }

  @Test
  void convertsPiecesWithPieceWeight() {
    ConversionFactors egg = ConversionFactors.withPieceWeight("50");

    assertEquals(Grams.of(150), egg.toGrams(new BigDecimal("3"), Unit.PIECE));
  }

  @Test
  void combinesDensityAndPieceWeight() {
    ConversionFactors factors = ConversionFactors.withDensity("1.03").andPieceWeight("1030");

    assertEquals(Grams.of(2060), factors.toGrams(new BigDecimal("2"), Unit.PIECE));
    assertTrue(factors.density().isPresent());
    assertTrue(factors.pieceWeight().isPresent());
  }

  @Test
  void failsWithoutDensityForVolume() {
    assertThrows(
        UnconvertibleQuantityException.class,
        () -> ConversionFactors.MASS_ONLY.toGrams(BigDecimal.ONE, Unit.CUP));
  }

  @Test
  void failsWithoutPieceWeightForCount() {
    assertThrows(
        UnconvertibleQuantityException.class,
        () -> ConversionFactors.withDensity("1").toGrams(BigDecimal.ONE, Unit.PIECE));
  }

  @Test
  void rejectsNonPositiveFactors() {
    assertThrows(IllegalArgumentException.class, () -> ConversionFactors.withDensity("0"));
    assertThrows(IllegalArgumentException.class, () -> ConversionFactors.withPieceWeight("-5"));
  }

  @Test
  void equalFactorsIgnoreTrailingZeros() {
    assertEquals(ConversionFactors.withDensity("1.03"), ConversionFactors.withDensity("1.030"));
  }
}
