package dev.haypacomer.application.sensor.validation;

import java.util.Objects;

public record ValidationResult(Verdict verdict, String reason) {

  public static final ValidationResult ACCEPTED = new ValidationResult(Verdict.ACCEPTED, "ok");

  public ValidationResult {
    Objects.requireNonNull(verdict, "verdict");
    Objects.requireNonNull(reason, "reason");
  }

  public static ValidationResult rejected(String reason) {
    return new ValidationResult(Verdict.REJECTED, reason);
  }

  public static ValidationResult duplicate() {
    return new ValidationResult(Verdict.DUPLICATE, "event already processed");
  }

  public static ValidationResult dropped(String reason) {
    return new ValidationResult(Verdict.DROPPED, reason);
  }

  public boolean accepted() {
    return verdict == Verdict.ACCEPTED;
  }
}
