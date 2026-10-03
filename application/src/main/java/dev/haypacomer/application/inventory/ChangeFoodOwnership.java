package dev.haypacomer.application.inventory;

import dev.haypacomer.application.port.FoodOwnershipRepository;
import dev.haypacomer.application.port.FridgeRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.InventoryMovementLog;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Membership;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.member.MemberId;
import java.util.function.Function;

public final class ChangeFoodOwnership {

  private final HouseholdInventory inventory;

  public ChangeFoodOwnership(
      HouseholdRepository households,
      FridgeRepository fridges,
      FoodOwnershipRepository ownerships,
      InventoryMovementLog movements) {
    this.inventory = new HouseholdInventory(households, fridges, ownerships, movements);
  }

  public Ownership change(
      UserId actor, HouseholdId householdId, FoodItemId itemId, OwnershipChange change) {
    Household household = inventory.household(actor, householdId);
    inventory.locate(householdId, itemId);
    Membership membership = household.membershipOf(actor).orElseThrow();
    Ownership current =
        inventory
            .ownerships()
            .find(itemId)
            .filter(ownership -> ownership.owner().equals(membership.member()))
            .orElseThrow(() -> new AccessDeniedException("Only the owner can change this food"));
    Ownership updated = change.apply(household, current);
    inventory.ownerships().save(itemId, updated);
    return updated;
  }

  public sealed interface OwnershipChange permits SetVisibility, Grant, Revoke {

    Ownership apply(Household household, Ownership current);
  }

  public record SetVisibility(Visibility visibility) implements OwnershipChange {

    @Override
    public Ownership apply(Household household, Ownership current) {
      return new Ownership(current.owner(), visibility, current.grantees());
    }
  }

  public record Grant(UserId user) implements OwnershipChange {

    @Override
    public Ownership apply(Household household, Ownership current) {
      return member(household, user, current::grant);
    }
  }

  public record Revoke(UserId user) implements OwnershipChange {

    @Override
    public Ownership apply(Household household, Ownership current) {
      return member(household, user, current::revoke);
    }
  }

  private static Ownership member(
      Household household, UserId user, Function<MemberId, Ownership> change) {
    return household
        .membershipOf(user)
        .map(Membership::member)
        .map(change)
        .orElseThrow(() -> new IllegalArgumentException("User is not a member of this household"));
  }
}
