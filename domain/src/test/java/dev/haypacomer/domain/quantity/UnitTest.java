package dev.haypacomer.domain.quantity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UnitTest {

  @Test
  void convertsToBaseUnit() {
    assertEquals(0, new BigDecimal("1500").compareTo(Unit.KILOGRAM.toBase(new BigDecimal("1.5"))));
    assertEquals(0, new BigDecimal("45").compareTo(Unit.TABLESPOON.toBase(new BigDecimal("3"))));
  }

  @Test
  void convertsFromBaseUnit() {
    assertEquals(0, new BigDecimal("0.25").compareTo(Unit.LITER.fromBase(new BigDecimal("250"))));
  }

  @Test
  void exposesKindAndSymbol() {
    assertEquals(UnitKind.MASS, Unit.POUND.kind());
    assertEquals(UnitKind.VOLUME, Unit.CUP.kind());
    assertEquals(UnitKind.COUNT, Unit.PIECE.kind());
    assertEquals("kg", Unit.KILOGRAM.symbol());
  }

  @Test
  void compatibilityFollowsKind() {
    assertTrue(Unit.GRAM.isCompatibleWith(Unit.OUNCE));
    assertFalse(Unit.GRAM.isCompatibleWith(Unit.MILLILITER));
  }
}
