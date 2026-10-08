package dev.haypacomer.domain.quantity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class QuantityParserTest {

  private static final ConversionFactors RICE = ConversionFactors.withDensity("0.85");
  private static final ConversionFactors EGG =
      ConversionFactors.withDensity("1.03").andPieceWeight("50");

  private static Grams grams(String text, ConversionFactors factors) {
    return QuantityParser.parse(text).interpret(factors);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "300 g|300",
        "300g|300",
        "1.5 kg|1500",
        "1,5 kilos|1500",
        "250 gramos de pollo|250",
        "2 lb|907.18474",
        "500 mg|0.5"
      })
  void readsMassInEnglishAndSpanish(String text, String expected) {
    assertEquals(Grams.of(expected), grams(text, ConversionFactors.MASS_ONLY));
  }

  @Test
  void convertsVolumeAndCountWithTheFoodFactors() {
    assertEquals(Grams.of("408"), grams("2 tazas de arroz", RICE));
    assertEquals(Grams.of("12.75"), grams("1 cda", RICE));
    assertEquals(Grams.of("150"), grams("3 huevos", EGG));
    assertEquals(Grams.of("100"), grams("2 unidades", EGG));
    assertEquals(Grams.of("1030"), grams("1 l", EGG));
  }

  @Test
  void readsFractionsAndMixedNumbers() {
    assertEquals(Grams.of("102"), grams("1/2 cup", RICE));
    assertEquals(Grams.of("306"), grams("1 1/2 tazas", RICE));
    assertEquals(Grams.of("250"), grams("1/4 kg", ConversionFactors.MASS_ONLY));
  }

  @Test
  void addsTermsWithMixedUnits() {
    QuantityExpression expression = QuantityParser.parse("1 kg + 200 g");

    assertInstanceOf(Sum.class, expression);
    assertEquals(Grams.of("1200"), expression.interpret(ConversionFactors.MASS_ONLY));
    assertEquals("1 kg + 200 g", expression.toString());
    assertEquals(Grams.of("242.25"), grams("1 taza y 3 cdas", RICE));
    assertEquals(Grams.of("1100"), grams("1 kg and 100 g", ConversionFactors.MASS_ONLY));
  }

  @Test
  void tellsWhenTheFoodLacksAConversionFactor() {
    assertThrows(
        UnconvertibleQuantityException.class, () -> grams("2 cups", ConversionFactors.MASS_ONLY));
    assertThrows(UnconvertibleQuantityException.class, () -> grams("3 eggs", RICE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "some rice", "300 g +", "1/0 cup", "kg 3"})
  void rejectsTextThatIsNotAQuantity(String text) {
    assertThrows(InvalidQuantityException.class, () -> QuantityParser.parse(text));
  }

  @Test
  void rejectsMissingAndOversizedText() {
    assertThrows(InvalidQuantityException.class, () -> QuantityParser.parse(null));
    assertThrows(InvalidQuantityException.class, () -> QuantityParser.parse("1 g + ".repeat(40)));
  }

  @Test
  void vocabularyIgnoresCaseAndAbbreviationDots() {
    assertEquals(Unit.TABLESPOON, UnitVocabulary.lookup(" Cda. ").orElseThrow());
    assertEquals(Unit.CUP, UnitVocabulary.lookup("CUPS").orElseThrow());
    assertTrue(UnitVocabulary.lookup("huevos").isEmpty());
    assertTrue(UnitVocabulary.lookup(null).isEmpty());
    assertEquals("2 cup", new Amount(Quantity.of("2", Unit.CUP)).toString());
  }
}
