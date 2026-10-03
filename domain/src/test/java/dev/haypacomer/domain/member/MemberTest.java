package dev.haypacomer.domain.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MemberTest {

  @Test
  void stripsDisplayName() {
    assertEquals("Ana", Member.named("  Ana ").displayName());
  }

  @Test
  void eachMemberGetsItsOwnId() {
    assertNotEquals(Member.named("Ana").id(), Member.named("Ana").id());
  }

  @Test
  void rejectsBlankOrMissingValues() {
    assertThrows(IllegalArgumentException.class, () -> Member.named(" "));
    assertThrows(NullPointerException.class, () -> Member.named(null));
    assertThrows(NullPointerException.class, () -> new Member(null, "Ana"));
    assertThrows(NullPointerException.class, () -> new MemberId(null));
  }
}
