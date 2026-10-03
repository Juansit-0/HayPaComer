package dev.haypacomer.domain.inventory;

import dev.haypacomer.domain.member.MemberId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record Ownership(MemberId owner, Visibility visibility, Set<MemberId> grantees) {

  public Ownership {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(visibility, "visibility");
    grantees = Set.copyOf(grantees);
  }

  public static Ownership of(MemberId owner, Visibility visibility) {
    return new Ownership(owner, visibility, Set.of());
  }

  public Ownership grant(MemberId member) {
    Set<MemberId> updated = new HashSet<>(grantees);
    updated.add(member);
    return new Ownership(owner, visibility, updated);
  }

  public Ownership revoke(MemberId member) {
    Set<MemberId> updated = new HashSet<>(grantees);
    updated.remove(member);
    return new Ownership(owner, visibility, updated);
  }

  public boolean allows(MemberId member) {
    return visibility == Visibility.SHARED || owner.equals(member) || grantees.contains(member);
  }
}
