package dev.haypacomer.domain.household;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HouseholdTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
  private static final Currency COP = Currency.getInstance("COP");
  private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final UserId guest = UserId.newId();
  private Household household;

  @BeforeEach
  void createHousehold() {
    household = Household.create(" Apartment 402 ", COP, BOGOTA, juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    household.join(guest, Role.GUEST, NOW);
  }

  @Test
  void creatorIsTheOwner() {
    assertEquals("Apartment 402", household.name());
    assertEquals(juan, household.owner());
    assertEquals(Role.OWNER, household.membershipOf(juan).orElseThrow().role());
    assertEquals(3, household.memberships().size());
    assertEquals(COP, household.currency());
    assertEquals(BOGOTA, household.timezone());
  }

  @Test
  void eachMembershipHasItsOwnMemberId() {
    assertNotEquals(
        household.membershipOf(juan).orElseThrow().member(),
        household.membershipOf(ana).orElseThrow().member());
  }

  @Test
  void rolesGrantPermissions() {
    assertTrue(household.can(juan, Permission.MANAGE_DEVICES));
    assertTrue(household.can(ana, Permission.EDIT_INVENTORY));
    assertFalse(household.can(ana, Permission.MANAGE_MEMBERS));
    assertTrue(household.can(guest, Permission.VIEW_HOUSEHOLD));
    assertFalse(household.can(guest, Permission.COOK));
    assertFalse(household.can(UserId.newId(), Permission.VIEW_HOUSEHOLD));
    assertTrue(Role.OWNER.permissions().containsAll(Role.MEMBER.permissions()));
  }

  @Test
  void requirePermissionDeniesOutsiders() {
    assertThrows(
        AccessDeniedException.class,
        () -> household.requirePermission(guest, Permission.EDIT_INVENTORY));
  }

  @Test
  void ownerChangesRolesButMembersCannot() {
    assertEquals(Role.MEMBER, household.changeRole(juan, guest, Role.MEMBER).role());
    assertThrows(AccessDeniedException.class, () -> household.changeRole(ana, guest, Role.GUEST));
    assertThrows(IllegalArgumentException.class, () -> household.changeRole(juan, ana, Role.OWNER));
    assertThrows(IllegalStateException.class, () -> household.changeRole(juan, juan, Role.GUEST));
  }

  @Test
  void joinRejectsDuplicatesAndOwnerRole() {
    assertThrows(IllegalStateException.class, () -> household.join(ana, Role.GUEST, NOW));
    assertThrows(
        IllegalArgumentException.class, () -> household.join(UserId.newId(), Role.OWNER, NOW));
  }

  @Test
  void membersLeaveOrAreRemovedButOwnerStays() {
    household.remove(guest, guest);
    household.remove(juan, ana);

    assertEquals(1, household.memberships().size());
    assertThrows(IllegalStateException.class, () -> household.remove(juan, juan));
    assertThrows(IllegalArgumentException.class, () -> household.remove(juan, ana));
  }

  @Test
  void memberCannotRemoveOthers() {
    assertThrows(AccessDeniedException.class, () -> household.remove(ana, guest));
  }

  @Test
  void ownershipTransfersToAnExistingMember() {
    household.transferOwnership(juan, ana);

    assertEquals(ana, household.owner());
    assertEquals(Role.MEMBER, household.membershipOf(juan).orElseThrow().role());
    assertThrows(AccessDeniedException.class, () -> household.transferOwnership(juan, guest));
    assertThrows(
        IllegalArgumentException.class, () -> household.transferOwnership(ana, UserId.newId()));
    household.transferOwnership(ana, ana);
    assertEquals(ana, household.owner());
  }

  @Test
  void ownerRenamesAndConfigures() {
    household.rename(juan, "Casa");
    household.configure(juan, Currency.getInstance("USD"), ZoneId.of("UTC"));

    assertEquals("Casa", household.name());
    assertEquals(Currency.getInstance("USD"), household.currency());
    assertThrows(AccessDeniedException.class, () -> household.rename(ana, "Mine"));
    assertThrows(IllegalArgumentException.class, () -> household.rename(juan, " "));
  }

  @Test
  void restoreRequiresExactlyOneOwner() {
    List<Membership> memberships = household.memberships();

    Household restored =
        Household.restore(household.id(), "Apartment 402", COP, BOGOTA, memberships);

    assertEquals(juan, restored.owner());
    assertThrows(
        IllegalStateException.class,
        () ->
            Household.restore(
                HouseholdId.newId(),
                "Broken",
                COP,
                BOGOTA,
                List.of(memberships.get(1), memberships.get(2))));
  }
}
