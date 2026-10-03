package dev.haypacomer.domain.member;

import java.util.Objects;

public record Member(MemberId id, String displayName) {

  public Member {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    displayName = displayName.strip();
    if (displayName.isEmpty()) {
      throw new IllegalArgumentException("Member name cannot be blank");
    }
  }

  public static Member named(String displayName) {
    return new Member(MemberId.newId(), displayName);
  }
}
