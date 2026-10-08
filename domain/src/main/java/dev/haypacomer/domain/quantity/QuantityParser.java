package dev.haypacomer.domain.quantity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuantityParser {

  private static final int MAX_LENGTH = 120;
  private static final Pattern TERM_SEPARATOR = Pattern.compile("\\s*\\+\\s*|\\s+(?:and|y)\\s+");
  private static final Pattern MIXED = Pattern.compile("^(\\d+)\\s+(\\d+)/(\\d+)(.*)$");
  private static final Pattern FRACTION = Pattern.compile("^(\\d+)/(\\d+)(.*)$");
  private static final Pattern DECIMAL = Pattern.compile("^(\\d+(?:[.,]\\d+)?)(.*)$");

  private QuantityParser() {}

  public static QuantityExpression parse(String text) {
    if (text == null || text.isBlank()) {
      throw new InvalidQuantityException("Quantity is empty");
    }
    if (text.length() > MAX_LENGTH) {
      throw new InvalidQuantityException("Quantity is too long");
    }
    QuantityExpression expression = null;
    for (String term : TERM_SEPARATOR.split(text.strip().toLowerCase(Locale.ROOT), -1)) {
      Amount amount = term(term);
      expression = expression == null ? amount : new Sum(expression, amount);
    }
    return expression;
  }

  private static Amount term(String term) {
    String text = term.strip();
    Matcher mixed = MIXED.matcher(text);
    if (mixed.matches()) {
      BigDecimal whole = new BigDecimal(mixed.group(1));
      return amount(whole.add(fraction(mixed.group(2), mixed.group(3), term)), mixed.group(4));
    }
    Matcher fraction = FRACTION.matcher(text);
    if (fraction.matches()) {
      return amount(fraction(fraction.group(1), fraction.group(2), term), fraction.group(3));
    }
    Matcher decimal = DECIMAL.matcher(text);
    if (decimal.matches()) {
      return amount(new BigDecimal(decimal.group(1).replace(',', '.')), decimal.group(2));
    }
    throw new InvalidQuantityException("Cannot read a quantity from \"" + term.strip() + "\"");
  }

  private static BigDecimal fraction(String numerator, String denominator, String term) {
    BigDecimal divisor = new BigDecimal(denominator);
    if (divisor.signum() == 0) {
      throw new InvalidQuantityException("Division by zero in \"" + term.strip() + "\"");
    }
    return new BigDecimal(numerator).divide(divisor, MathContext.DECIMAL64);
  }

  private static Amount amount(BigDecimal value, String rest) {
    String words = rest.strip();
    String first = words.isEmpty() ? "" : words.split("\\s+")[0];
    Unit unit = UnitVocabulary.lookup(first).orElse(Unit.PIECE);
    return new Amount(new Quantity(value, unit));
  }
}
