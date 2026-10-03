package dev.haypacomer.domain.household;

import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.MemberId;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Household {

  private final HouseholdId id;
  private final Map<UserId, Membership> memberships = new LinkedHashMap<>();
  private String name;
  private Currency currency;
  private ZoneId timezone;

  private Household(HouseholdId id, String name, Currency currency, ZoneId timezone) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = requireName(name);
    this.currency = Objects.requireNonNull(currency, "currency");
    this.timezone = Objects.requireNonNull(timezone, "timezone");
  }

  public static Household create(
      String name, Currency currency, ZoneId timezone, UserId owner, Instant now) {
    Household household = new Household(HouseholdId.newId(), name, currency, timezone);
    household.memberships.put(owner, new Membership(owner, MemberId.newId(), Role.OWNER, now));
    return household;
  }

  public static Household restore(
      HouseholdId id,
      String name,
      Currency currency,
      ZoneId timezone,
      List<Membership> memberships) {
    Household household = new Household(id, name, currency, timezone);
    memberships.forEach(membership -> household.memberships.put(membership.user(), membership));
    long owners = memberships.stream().filter(m -> m.role() == Role.OWNER).count();
    if (owners != 1) {
      throw new IllegalStateException("A household needs exactly one owner, found " + owners);
    }
    return household;
  }

  public HouseholdId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public Currency currency() {
    return currency;
  }

  public ZoneId timezone() {
    return timezone;
  }

  public List<Membership> memberships() {
    return List.copyOf(memberships.values());
  }

  public Optional<Membership> membershipOf(UserId user) {
    return Optional.ofNullable(memberships.get(user));
  }

  public UserId owner() {
    return memberships.values().stream()
        .filter(membership -> membership.role() == Role.OWNER)
        .map(Membership::user)
        .findFirst()
        .orElseThrow();
  }

  public boolean can(UserId user, Permission permission) {
    return membershipOf(user).map(membership -> membership.can(permission)).orElse(false);
  }

  public void requirePermission(UserId user, Permission permission) {
    if (!can(user, permission)) {
      throw new AccessDeniedException("Missing permission " + permission + " in household " + name);
    }
  }

  public void rename(UserId actor, String newName) {
    requirePermission(actor, Permission.MANAGE_HOUSEHOLD);
    name = requireName(newName);
  }

  public void configure(UserId actor, Currency newCurrency, ZoneId newTimezone) {
    requirePermission(actor, Permission.MANAGE_HOUSEHOLD);
    currency = Objects.requireNonNull(newCurrency, "currency");
    timezone = Objects.requireNonNull(newTimezone, "timezone");
  }

  public Membership join(UserId user, Role role, Instant now) {
    Objects.requireNonNull(user, "user");
    if (role == Role.OWNER) {
      throw new IllegalArgumentException("Ownership is only granted by transfer");
    }
    if (memberships.containsKey(user)) {
      throw new IllegalStateException("User already belongs to household " + name);
    }
    Membership membership = new Membership(user, MemberId.newId(), role, now);
    memberships.put(user, membership);
    return membership;
  }

  public Membership changeRole(UserId actor, UserId target, Role newRole) {
    requirePermission(actor, Permission.MANAGE_MEMBERS);
    if (newRole == Role.OWNER) {
      throw new IllegalArgumentException("Use transferOwnership to change the owner");
    }
    Membership current = requireMembership(target);
    if (current.role() == Role.OWNER) {
      throw new IllegalStateException("The owner role changes only through transferOwnership");
    }
    Membership updated = current.withRole(newRole);
    memberships.put(target, updated);
    return updated;
  }

  public void remove(UserId actor, UserId target) {
    Membership membership = requireMembership(target);
    if (!actor.equals(target)) {
      requirePermission(actor, Permission.MANAGE_MEMBERS);
    }
    if (membership.role() == Role.OWNER) {
      throw new IllegalStateException("The owner cannot leave; transfer ownership first");
    }
    memberships.remove(target);
  }

  public void transferOwnership(UserId actor, UserId newOwner) {
    if (!owner().equals(actor)) {
      throw new AccessDeniedException("Only the owner can transfer ownership");
    }
    Membership target = requireMembership(newOwner);
    if (target.user().equals(actor)) {
      return;
    }
    memberships.put(actor, requireMembership(actor).withRole(Role.MEMBER));
    memberships.put(newOwner, target.withRole(Role.OWNER));
  }

  private Membership requireMembership(UserId user) {
    return membershipOf(user)
        .orElseThrow(() -> new IllegalArgumentException("User is not a member of " + name));
  }

  private static String requireName(String name) {
    Objects.requireNonNull(name, "name");
    String stripped = name.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException("Household name cannot be blank");
    }
    return stripped;
  }
}
