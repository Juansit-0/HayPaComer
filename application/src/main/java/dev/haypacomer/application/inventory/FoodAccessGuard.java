package dev.haypacomer.application.inventory;

import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Membership;
import dev.haypacomer.domain.household.Permission;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.member.MemberId;
import java.util.Optional;

public final class FoodAccessGuard {

  public MemberId requireUsable(Household household, UserId actor, Optional<Ownership> ownership) {
    Membership membership =
        household
            .membershipOf(actor)
            .orElseThrow(() -> new AccessDeniedException("Not a member of " + household.name()));
    MemberId member = membership.member();
    if (ownership.isEmpty() || ownership.get().visibility() == Visibility.SHARED) {
      household.requirePermission(actor, Permission.EDIT_INVENTORY);
      return member;
    }
    Ownership owned = ownership.get();
    if (owned.owner().equals(member)) {
      household.requirePermission(actor, Permission.MANAGE_OWN_ITEMS);
      return member;
    }
    if (owned.allows(member)) {
      household.requirePermission(actor, Permission.EDIT_INVENTORY);
      return member;
    }
    if (owned.visibility() == Visibility.ASK_FIRST) {
      throw new PermissionRequiredException();
    }
    throw new AccessDeniedException("This food is private");
  }
}
