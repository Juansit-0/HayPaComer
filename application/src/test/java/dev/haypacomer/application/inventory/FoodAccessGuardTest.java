package dev.haypacomer.application.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.Ownership;
import dev.haypacomer.domain.inventory.Visibility;
import dev.haypacomer.domain.member.MemberId;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FoodAccessGuardTest {

  private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  private final FoodAccessGuard guard = new FoodAccessGuard();
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final UserId guest = UserId.newId();
  private Household household;
  private MemberId juanMember;
  private MemberId anaMember;
  private MemberId guestMember;

  @BeforeEach
  void createHousehold() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    household.join(guest, Role.GUEST, NOW);
    juanMember = household.membershipOf(juan).orElseThrow().member();
    anaMember = household.membershipOf(ana).orElseThrow().member();
    guestMember = household.membershipOf(guest).orElseThrow().member();
  }

  @Test
  void sharedFoodIsForMembersWhoCanEditInventory() {
    assertEquals(anaMember, guard.requireUsable(household, ana, Optional.empty()));
    assertEquals(
        anaMember,
        guard.requireUsable(
            household, ana, Optional.of(Ownership.of(juanMember, Visibility.SHARED))));
    assertThrows(
        AccessDeniedException.class, () -> guard.requireUsable(household, guest, Optional.empty()));
  }

  @Test
  void ownersUseTheirOwnFoodEvenAsGuests() {
    assertEquals(
        guestMember,
        guard.requireUsable(
            household, guest, Optional.of(Ownership.of(guestMember, Visibility.PRIVATE))));
  }

  @Test
  void privateFoodNeedsAGrant() {
    Ownership yogurt = Ownership.of(juanMember, Visibility.PRIVATE);

    assertThrows(
        AccessDeniedException.class,
        () -> guard.requireUsable(household, ana, Optional.of(yogurt)));
    assertEquals(
        anaMember, guard.requireUsable(household, ana, Optional.of(yogurt.grant(anaMember))));
  }

  @Test
  void askFirstFoodAsksForPermission() {
    assertThrows(
        PermissionRequiredException.class,
        () ->
            guard.requireUsable(
                household, ana, Optional.of(Ownership.of(juanMember, Visibility.ASK_FIRST))));
  }

  @Test
  void outsidersAreDenied() {
    assertThrows(
        AccessDeniedException.class,
        () -> guard.requireUsable(household, UserId.newId(), Optional.empty()));
  }
}
