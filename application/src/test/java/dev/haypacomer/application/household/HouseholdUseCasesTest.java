package dev.haypacomer.application.household;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.household.AccessDeniedException;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HouseholdUseCasesTest {

  private final InMemoryHouseholds repository = new InMemoryHouseholds();
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC);
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final UserId stranger = UserId.newId();
  private Household apartment;

  @BeforeEach
  void createApartment() {
    apartment =
        new CreateHousehold(repository, clock)
            .create(juan, new CreateHouseholdCommand("Apartment 402", "COP", "America/Bogota"));
    apartment.join(ana, Role.MEMBER, clock.instant());
  }

  @Test
  void createsHouseholdOwnedByTheActor() {
    assertEquals(juan, apartment.owner());
    assertEquals(Currency.getInstance("COP"), apartment.currency());
    assertEquals(ZoneId.of("America/Bogota"), apartment.timezone());
    assertEquals(apartment, repository.findById(apartment.id()).orElseThrow());
  }

  @Test
  void rejectsUnknownCurrencyOrTimezone() {
    CreateHousehold create = new CreateHousehold(repository, clock);

    assertThrows(
        IllegalArgumentException.class,
        () -> create.create(juan, new CreateHouseholdCommand("X", "XXX1", "UTC")));
    assertThrows(
        IllegalArgumentException.class,
        () -> create.create(juan, new CreateHouseholdCommand("X", "COP", "Mars/Base")));
    assertThrows(
        IllegalArgumentException.class,
        () -> create.create(juan, new CreateHouseholdCommand("X", null, null)));
  }

  @Test
  void listsOnlyTheActorsHouseholds() {
    assertEquals(List.of(apartment), new ListHouseholds(repository).list(ana));
    assertTrue(new ListHouseholds(repository).list(stranger).isEmpty());
  }

  @Test
  void nonMembersCannotEvenSeeTheHousehold() {
    GetHousehold get = new GetHousehold(repository);

    assertEquals(apartment, get.get(ana, apartment.id()));
    assertThrows(HouseholdNotFoundException.class, () -> get.get(stranger, apartment.id()));
    assertThrows(HouseholdNotFoundException.class, () -> get.get(juan, HouseholdId.newId()));
  }

  @Test
  void ownerUpdatesSettingsPartially() {
    UpdateHousehold update = new UpdateHousehold(repository);

    update.update(juan, apartment.id(), new UpdateHouseholdCommand("Casa", null, null));
    update.update(juan, apartment.id(), new UpdateHouseholdCommand(null, "USD", null));
    update.update(juan, apartment.id(), new UpdateHouseholdCommand(null, null, "UTC"));
    update.update(juan, apartment.id(), new UpdateHouseholdCommand(null, null, null));

    assertEquals("Casa", apartment.name());
    assertEquals(Currency.getInstance("USD"), apartment.currency());
    assertEquals(ZoneId.of("UTC"), apartment.timezone());
    assertThrows(
        AccessDeniedException.class,
        () -> update.update(ana, apartment.id(), new UpdateHouseholdCommand("Mine", null, null)));
  }

  @Test
  void managesMembersThroughTheAggregateRules() {
    UserId guest = UserId.newId();
    apartment.join(guest, Role.GUEST, clock.instant());

    assertEquals(
        Role.MEMBER,
        new ChangeMemberRole(repository).change(juan, apartment.id(), guest, Role.MEMBER).role());
    assertThrows(
        AccessDeniedException.class,
        () -> new ChangeMemberRole(repository).change(ana, apartment.id(), guest, Role.GUEST));

    new RemoveMember(repository).remove(juan, apartment.id(), guest);
    assertTrue(apartment.membershipOf(guest).isEmpty());

    Household transferred = new TransferOwnership(repository).transfer(juan, apartment.id(), ana);
    assertEquals(ana, transferred.owner());
    assertThrows(
        HouseholdNotFoundException.class,
        () -> new RemoveMember(repository).remove(stranger, apartment.id(), ana));
  }
}
