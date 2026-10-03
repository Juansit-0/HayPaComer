package dev.haypacomer.domain.member;

import static dev.haypacomer.domain.member.FoodProfileTest.CHICKEN;
import static dev.haypacomer.domain.member.FoodProfileTest.RICE;
import static dev.haypacomer.domain.member.FoodProfileTest.TUNA;
import static dev.haypacomer.domain.member.FoodProfileTest.recipe;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.Allergen;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiningGroupTest {

  private final MemberId juan = MemberId.newId();
  private final MemberId ana = MemberId.newId();
  private final DiningGroup household =
      DiningGroup.of(
          FoodProfile.omnivore(juan),
          FoodProfile.omnivore(ana).withAllergies(Set.of(Allergen.FISH)));

  @Test
  void everyMemberMustAllowTheFood() {
    assertTrue(household.allows(CHICKEN));
    assertFalse(household.allows(TUNA));
  }

  @Test
  void recipeIsSharedOnlyWithoutConflicts() {
    assertTrue(household.canShare(recipe(CHICKEN, RICE)));
    assertFalse(household.canShare(recipe(TUNA, RICE)));
  }

  @Test
  void conflictsNameTheMember() {
    List<ProfileConflict> conflicts = household.conflictsWith(recipe(TUNA, RICE));

    assertEquals(1, conflicts.size());
    assertEquals(ana, conflicts.getFirst().member());
  }

  @Test
  void rejectsDuplicateMembers() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DiningGroup.of(FoodProfile.omnivore(juan), FoodProfile.omnivore(juan)));
  }
}
