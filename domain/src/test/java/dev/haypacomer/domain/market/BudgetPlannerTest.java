package dev.haypacomer.domain.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.Unit;
import dev.haypacomer.domain.substitution.SubstitutionRule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BudgetPlannerTest {

  private static final Instant MONTH = Instant.parse("2026-10-01T05:00:00Z");
  private static final Instant NOW = Instant.parse("2026-10-09T18:00:00Z");
  private static final UserId JUAN = UserId.newId();
  private static final FoodMetadata BEEF = food("Ground beef");
  private static final FoodMetadata CHICKEN = food("Chicken breast");
  private static final FoodMetadata RICE = food("Rice");
  private static final FoodMetadata MILK = food("Milk");
  private static final FoodMetadata SAFFRON = food("Saffron");
  private static final Map<String, BigDecimal> PRICES =
      Map.of(
          "ground beef", new BigDecimal("30000"),
          "chicken breast", new BigDecimal("22000"),
          "rice", new BigDecimal("4800"),
          "milk", new BigDecimal("4800"));
  private static final List<SubstitutionRule> RULES =
      List.of(SubstitutionRule.of(BEEF, CHICKEN, "1", 1000));

  private static FoodMetadata food(String name) {
    return new FoodMetadata(
        name, FoodCategory.OTHER, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 5, Set.of());
  }

  private static MarketList list() {
    MarketList list = MarketList.empty(HouseholdId.newId());
    MarketItem bought =
        list.add(MILK, Grams.of(2000), MarketSource.MANUAL, JUAN, MONTH.minusSeconds(60));
    list.check(bought.id(), NOW.minusSeconds(3600));
    MarketItem old =
        list.add(RICE, Grams.of(5000), MarketSource.MANUAL, JUAN, MONTH.minusSeconds(120));
    list.check(old.id(), MONTH.minusSeconds(60));
    list.add(MILK, Grams.of(1000), MarketSource.MANUAL, JUAN, NOW.minusSeconds(30));
    list.add(BEEF, Grams.of(500), MarketSource.MANUAL, JUAN, NOW.minusSeconds(20));
    list.add(RICE, Grams.of(1000), MarketSource.PLAN, JUAN, NOW.minusSeconds(10));
    list.add(SAFFRON, Grams.of(5), MarketSource.MANUAL, JUAN, NOW);
    return list;
  }

  @Test
  void planNeedsComeFirstAndTheRestFillsWhatIsLeftOfTheMonth() {
    BudgetPlan plan =
        new BudgetPlanner().plan(list(), new BigDecimal("25000"), MONTH, PRICES, RULES);

    assertEquals(new BigDecimal("9600.00"), plan.spent());
    assertEquals(new BigDecimal("15400.00"), plan.remaining());
    assertEquals(
        List.of("rice", "milk", "ground beef", "saffron"),
        plan.lines().stream().map(line -> line.item().food().key()).toList());
    assertTrue(plan.lines().get(0).withinBudget());
    assertTrue(plan.lines().get(1).withinBudget());
    assertEquals(new BigDecimal("9600.00"), plan.plannedCost());
    BudgetLine beef = plan.overBudget().getFirst();
    assertEquals(new BigDecimal("15000.00"), beef.cost().orElseThrow());
    CheaperOption chicken = beef.cheaperOption().orElseThrow();
    assertEquals("Chicken breast", chicken.substitute().name());
    assertEquals(new BigDecimal("11000.00"), chicken.estimatedCost());
    assertEquals("saffron", plan.unpriced().getFirst().item().food().key());
    assertTrue(plan.unpriced().getFirst().cheaperOption().isEmpty());
  }

  @Test
  void anOverspentMonthLeavesNothingButStillListsTheNeeds() {
    BudgetPlan plan =
        new BudgetPlanner().plan(list(), new BigDecimal("5000"), MONTH, PRICES, List.of());

    assertEquals(BigDecimal.ZERO.setScale(2), plan.remaining());
    assertEquals(BigDecimal.ZERO.setScale(2), plan.plannedCost());
    assertEquals(3, plan.overBudget().size());
    assertFalse(plan.lines().getFirst().withinBudget());
    assertThrows(
        IllegalArgumentException.class,
        () -> new BudgetPlanner().plan(list(), new BigDecimal("-1"), MONTH, PRICES, RULES));
  }
}
