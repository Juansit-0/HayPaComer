package dev.haypacomer.domain.analytics;

import dev.haypacomer.domain.identity.UserId;
import java.util.Objects;

public record MemberTally(UserId user, Tally tally) {

  public MemberTally {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(tally, "tally");
  }
}
