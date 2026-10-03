package dev.haypacomer.domain.quantity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuantityTest {

  @Test
  void convertsBetweenCompatibleUnits() {
    Quantity converted = Quantity.of("1.5", Unit.KILOGRAM).convertTo(Unit.GRAM);

    assertEquals(Unit.GRAM, converted.unit());
    assertEquals(0, new BigDecimal("1500").compareTo(converted.amount()));
  }

  @Test
  void rejectsConversionBetweenKinds() {
    assertThrows(
        UnconvertibleQuantityException.class,
        () -> Quantity.of("1", Unit.LITER).convertTo(Unit.GRAM));
  }

  @Test
  void convertsMassToGramsWithoutFactors() {
    assertEquals(
        Grams.of("453.59"), Quantity.of("1", Unit.POUND).toGrams(ConversionFactors.MASS_ONLY));
  }

  @Test
  void rejectsNegativeAmount() {
    assertThrows(IllegalArgumentException.class, () -> Quantity.of("-2", Unit.GRAM));
  }

  @Test
  void rejectsMissingUnit() {
    assertThrows(NullPointerException.class, () -> new Quantity(BigDecimal.ONE, null));
  }

  @Test
  void printsAmountAndSymbol() {
    assertEquals("300 g", Quantity.of("300.00", Unit.GRAM).toString());
  }
}
