package dev.haypacomer.domain.substitution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.DiningGroup;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubstitutionCatalogTest {

  private static final FoodMetadata BUTTER = food("Butter", FoodCategory.DAIRY, Allergen.MILK);
  private static final FoodMetadata OIL = food("Vegetable oil", FoodCategory.CONDIMENT);
  private static final FoodMetadata GHEE = food("Ghee", FoodCategory.DAIRY, Allergen.MILK);
  private static final FoodMetadata CHICKEN = food("Chicken breast", FoodCategory.POULTRY);

  private static final SubstitutionRule BUTTER_TO_GHEE =
      SubstitutionRule.of(BUTTER, GHEE, "1", 100);
  private static final SubstitutionRule BUTTER_TO_OIL = SubstitutionRule.of(BUTTER, OIL, "0.8", 50);
  private static final SubstitutionCatalog CATALOG =
      new SubstitutionCatalog(
          List.of(BUTTER_TO_GHEE, BUTTER_TO_OIL, SubstitutionRule.of(CHICKEN, OIL, "1", 10)));

  private static final DiningGroup ANYONE = DiningGroup.of(FoodProfile.omnivore(MemberId.newId()));
  private static final DiningGroup LACTOSE_FREE =
      DiningGroup.of(
          new FoodProfile(MemberId.newId(), Diet.OMNIVORE, Set.of(Allergen.MILK), Set.of()));

  private static FoodMetadata food(String name, FoodCategory category, Allergen... allergens) {
    return new FoodMetadata(
        name, category, Unit.GRAM, ConversionFactors.MASS_ONLY, false, 30, Set.of(allergens));
  }

  @Test
  void appliesTheProportionToTheMissingGrams() {
    Substitution substitution =
        CATALOG
            .propose(
                BUTTER,
                Grams.of(40),
                food -> Map.of(OIL, Grams.of(32)).getOrDefault(food, Grams.ZERO),
                LACTOSE_FREE)
            .orElseThrow();

    assertEquals(OIL, substitution.substitute());
    assertEquals(Grams.of(40), substitution.replaced());
    assertEquals(Grams.of(32), substitution.substituteGrams());
    assertEquals(2, CATALOG.rulesFor(BUTTER).size());
  }

  @Test
  void prefersTheFirstRuleThatPassesEveryCheck() {
    Map<FoodMetadata, Grams> stock = Map.of(GHEE, Grams.of(500), OIL, Grams.of(500));

    assertEquals(
        GHEE, CATALOG.propose(BUTTER, Grams.of(40), stock::get, ANYONE).orElseThrow().substitute());
    assertEquals(
        OIL,
        CATALOG.propose(BUTTER, Grams.of(40), stock::get, LACTOSE_FREE).orElseThrow().substitute());
    assertTrue(CATALOG.propose(BUTTER, Grams.of(120), stock::get, ANYONE).isEmpty());
  }

  @Test
  void reportsEveryProblemWithARule() {
    assertEquals(
        Set.of(
            SubstitutionProblem.OVER_LIMIT,
            SubstitutionProblem.NOT_ENOUGH_STOCK,
            SubstitutionProblem.CONFLICTS_WITH_DINERS),
        CATALOG.check(BUTTER_TO_GHEE, Grams.of(150), Grams.of(149), LACTOSE_FREE));
    assertEquals(
        Set.of(SubstitutionProblem.NOT_ENOUGH_STOCK),
        CATALOG.check(BUTTER_TO_OIL, Grams.of(50), Grams.of(39), LACTOSE_FREE));
    assertTrue(CATALOG.check(BUTTER_TO_OIL, Grams.of(50), Grams.of(40), ANYONE).isEmpty());
    assertTrue(SubstitutionCatalog.EMPTY.rulesFor(BUTTER).isEmpty());
  }

  @Test
  void rulesAreValid() {
    assertTrue(BUTTER_TO_OIL.replaces(food("butter", FoodCategory.DAIRY)));
    assertFalse(BUTTER_TO_OIL.replaces(OIL));
    assertThrows(IllegalArgumentException.class, () -> SubstitutionRule.of(BUTTER, BUTTER, "1", 1));
    assertThrows(IllegalArgumentException.class, () -> SubstitutionRule.of(BUTTER, OIL, "0", 1));
    assertThrows(IllegalArgumentException.class, () -> SubstitutionRule.of(BUTTER, OIL, "1", 0));
    assertEquals(new BigDecimal("0.8"), BUTTER_TO_OIL.ratio());
  }
}
