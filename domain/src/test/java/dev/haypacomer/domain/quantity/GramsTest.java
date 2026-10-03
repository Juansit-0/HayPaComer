package dev.haypacomer.domain.quantity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GramsTest {

  @Test
  void normalizesScaleSoEqualValuesAreEqual() {
    assertEquals(Grams.of("842"), Grams.of("842.000"));
    assertEquals(Grams.of(842), Grams.of(new BigDecimal("842.0")));
  }

  @Test
  void roundsToTwoDecimals() {
    assertEquals(Grams.of("0.13"), Grams.of("0.125"));
  }

  @Test
  void rejectsNegativeValues() {
    assertThrows(IllegalArgumentException.class, () -> Grams.of(-1));
  }

  @Test
  void rejectsNullValue() {
    assertThrows(NullPointerException.class, () -> Grams.of((BigDecimal) null));
  }

  @Test
  void discountsMeasuredConsumption() {
    assertEquals(Grams.of(192), Grams.of(842).minus(Grams.of(650)));
  }

  @Test
  void addsQuantities() {
    assertEquals(Grams.of(300), Grams.of(130).plus(Grams.of(170)));
  }

  @Test
  void cannotSubtractMoreThanAvailable() {
    assertThrows(IllegalArgumentException.class, () -> Grams.of(80).minus(Grams.of(200)));
  }

  @Test
  void scalesByFactor() {
    assertEquals(Grams.of(100), Grams.of(200).times(new BigDecimal("0.5")));
  }

  @Test
  void computesShortfallAgainstRequirement() {
    assertEquals(Grams.of(120), Grams.of(80).shortfallTo(Grams.of(200)));
    assertEquals(Grams.ZERO, Grams.of(250).shortfallTo(Grams.of(200)));
  }

  @Test
  void comparesQuantities() {
    assertTrue(Grams.of(200).isAtLeast(Grams.of(200)));
    assertFalse(Grams.of(80).isAtLeast(Grams.of(200)));
    assertTrue(Grams.of(80).compareTo(Grams.of(200)) < 0);
    assertTrue(Grams.ZERO.isZero());
    assertFalse(Grams.of(1).isZero());
  }

  @Test
  void printsCompactly() {
    assertEquals("192 g", Grams.of(192).toString());
    assertEquals("12.5 g", Grams.of("12.50").toString());
  }
}
