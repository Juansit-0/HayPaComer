package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.CHICKEN;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.EGG;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.MILK;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.member.Diet;
import dev.haypacomer.domain.member.FoodProfile;
import dev.haypacomer.domain.member.MemberId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PostgresSubstitutionAndProfileTest extends PostgresTestSupport {

  @Test
  void keepsRulesInTheOrderTheyWereAdded() {
    PostgresFoodCatalogRepository catalog = new PostgresFoodCatalogRepository(dataSource);
    List.of(MILK, EGG, CHICKEN).forEach(catalog::save);
    PostgresSubstitutionRuleRepository rules = new PostgresSubstitutionRuleRepository(dataSource);
    SubstitutionRule first = SubstitutionRule.of(CHICKEN, EGG, "1.25", 200);
    SubstitutionRule second = SubstitutionRule.of(MILK, EGG, "0.5", 100);

    rules.save(first);
    rules.save(second);
    rules.save(
        new SubstitutionRule(first.id(), CHICKEN, EGG, new BigDecimal("1.5"), Grams.of(250)));

    List<SubstitutionRule> loaded = rules.all();
    assertEquals(2, loaded.size());
    assertEquals(first.id(), loaded.getFirst().id());
    assertEquals(new BigDecimal("1.5"), loaded.getFirst().ratio());
    assertEquals(Grams.of(250), loaded.getFirst().maxReplaced());
    assertEquals(EGG.key(), loaded.getFirst().substitute().key());
    assertEquals(Set.of(Allergen.EGGS), loaded.getFirst().substitute().allergens());
    assertEquals(MILK.key(), loaded.get(1).original().key());
  }

  @Test
  void hasNoRulesWithoutACatalog() {
    assertTrue(new PostgresSubstitutionRuleRepository(dataSource).all().isEmpty());
  }

  @Test
  void savesAndReplacesFoodProfiles() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    MemberId member = household.membershipOf(juan.id()).orElseThrow().member();
    PostgresFoodProfileRepository profiles = new PostgresFoodProfileRepository(dataSource);

    assertTrue(profiles.find(member).isEmpty());
    profiles.save(
        new FoodProfile(
            member, Diet.VEGETARIAN, Set.of(Allergen.PEANUTS, Allergen.MILK), Set.of("Okra")));
    assertEquals(
        new FoodProfile(
            member, Diet.VEGETARIAN, Set.of(Allergen.PEANUTS, Allergen.MILK), Set.of("okra")),
        profiles.find(member).orElseThrow());

    FoodProfile vegan = new FoodProfile(member, Diet.VEGAN, Set.of(), Set.of());
    profiles.save(vegan);
    assertEquals(vegan, profiles.find(member).orElseThrow());
  }
}
