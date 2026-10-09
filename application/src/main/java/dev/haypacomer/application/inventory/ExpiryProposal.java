package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.expiry.ExpirySource;
import java.time.LocalDate;
import java.util.Objects;

public record ExpiryProposal(
    String food,
    String productOnLabel,
    LocalDate printedDate,
    LocalDate expiresOn,
    ExpirySource source,
    double confidence,
    String reason) {

  public ExpiryProposal {
    Objects.requireNonNull(food, "food");
    Objects.requireNonNull(expiresOn, "expiresOn");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(reason, "reason");
  }
}
