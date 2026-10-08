package dev.haypacomer.domain.substitution;

import java.util.Objects;
import java.util.UUID;

public record SubstitutionRuleId(UUID value) {

  public SubstitutionRuleId {
    Objects.requireNonNull(value, "value");
  }

  public static SubstitutionRuleId newId() {
    return new SubstitutionRuleId(UUID.randomUUID());
  }
}
