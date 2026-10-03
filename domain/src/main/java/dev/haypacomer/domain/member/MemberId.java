package dev.haypacomer.domain.member;

import java.util.Objects;
import java.util.UUID;

public record MemberId(UUID value) {

  public MemberId {
    Objects.requireNonNull(value, "value");
  }

  public static MemberId newId() {
    return new MemberId(UUID.randomUUID());
  }
}
