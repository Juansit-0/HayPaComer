package dev.haypacomer.domain.household;

import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.MemberId;
import java.time.Instant;
import java.util.Objects;

public record Membership(UserId user, MemberId member, Role role, Instant joinedAt) {

  public Membership {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(member, "member");
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(joinedAt, "joinedAt");
  }

  public Membership withRole(Role newRole) {
    return new Membership(user, member, newRole, joinedAt);
  }

  public boolean can(Permission permission) {
    return role.grants(permission);
  }
}
