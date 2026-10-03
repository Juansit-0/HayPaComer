package dev.haypacomer.domain.identity;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

  private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final int MAX_LENGTH = 254;

  public EmailAddress {
    Objects.requireNonNull(value, "value");
    value = value.strip().toLowerCase(Locale.ROOT);
    if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid email address");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
