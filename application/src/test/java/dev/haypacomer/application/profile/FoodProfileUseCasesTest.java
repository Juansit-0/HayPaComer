package dev.haypacomer.application.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.support.InMemoryCookingStores;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.FoodProfile;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FoodProfileUseCasesTest {

  private static final Instant NOW = Instant.parse("2026-10-08T17:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryCookingStores cooking = new InMemoryCookingStores();
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private final Household household =
      Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);

  @Test
  void membersDeclareTheirOwnProfileAndSeeEveryone() {
    household.join(ana, Role.MEMBER, NOW);
    households.save(household);

    FoodProfile anaProfile =
        new UpdateFoodProfile(households, cooking.profiles)
            .update(ana, household.id(), Diet.VEGETARIAN, Set.of(Allergen.PEANUTS), Set.of("Okra"));
    List<FoodProfile> profiles =
        new ListFoodProfiles(households, cooking.profiles).list(juan, household.id());

    assertEquals(household.membershipOf(ana).orElseThrow().member(), anaProfile.member());
    assertEquals(2, profiles.size());
    assertEquals(Diet.OMNIVORE, profiles.getFirst().diet());
    assertEquals(Set.of(Allergen.PEANUTS), profiles.get(1).allergies());
    assertEquals(Set.of("okra"), profiles.get(1).avoidedFoods());
  }

  @Test
  void outsidersCannotReadOrWriteProfiles() {
    households.save(household);

    assertThrows(
        HouseholdNotFoundException.class,
        () ->
            new UpdateFoodProfile(households, cooking.profiles)
                .update(ana, household.id(), Diet.VEGAN, Set.of(), Set.of()));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> new ListFoodProfiles(households, cooking.profiles).list(ana, household.id()));
  }
}
