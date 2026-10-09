package dev.haypacomer.domain.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WeeklyCoachTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
  private static final HouseholdId HOME = HouseholdId.newId();
  private static final UserId JUAN = UserId.newId();
  private static final UserId ANA = UserId.newId();

  private static InventoryMovement move(
      UserId actor, MovementType type, long grams, String food, LocalDate day, LocalDate expiry) {
    return new InventoryMovement(
        UUID.randomUUID(),
        HOME,
        FoodItemId.newId(),
        actor,
        type,
        BigDecimal.valueOf(-grams),
        MovementSource.MANUAL,
        day.atTime(12, 0).toInstant(ZoneOffset.UTC),
        food,
        expiry);
  }

  @Test
  void foodsThrownAwayTwiceBecomeBuyLessTips() {
    List<InventoryMovement> movements = new ArrayList<>();
    movements.add(move(JUAN, MovementType.DISCARD, 300, "bread", MONDAY, null));
    movements.add(move(JUAN, MovementType.DISCARD, 200, "bread", MONDAY.plusDays(2), null));
    movements.add(move(JUAN, MovementType.CONSUME, 500, "bread", MONDAY.plusDays(1), null));
    movements.add(move(JUAN, MovementType.DISCARD, 900, "milk", MONDAY, null));
    movements.add(move(JUAN, MovementType.DISCARD, 50, "milk", MONDAY.plusDays(3), null));
    movements.add(move(JUAN, MovementType.DISCARD, 400, "rice", MONDAY, null));
    movements.add(move(JUAN, MovementType.DISCARD, 10, "a", MONDAY, null));
    movements.add(move(JUAN, MovementType.DISCARD, 10, "a", MONDAY, null));
    movements.add(move(JUAN, MovementType.DISCARD, 5, "b", MONDAY, null));
    movements.add(move(JUAN, MovementType.DISCARD, 5, "b", MONDAY, null));
    movements.add(
        new InventoryMovement(
            UUID.randomUUID(),
            HOME,
            FoodItemId.newId(),
            JUAN,
            MovementType.DISCARD,
            new BigDecimal("-70"),
            MovementSource.MANUAL,
            MONDAY.atStartOfDay().toInstant(ZoneOffset.UTC)));

    List<WasteTip> tips = WastePatterns.find(movements);

    assertEquals(List.of("milk", "bread", "a"), tips.stream().map(WasteTip::foodKey).toList());
    assertEquals(90, tips.getFirst().buyLessPercent());
    assertEquals(50, tips.get(1).buyLessPercent());
    assertEquals(
        "You threw away bread 2 times (500 g). Buy about 50% less, or plan a dish for it earlier.",
        tips.get(1).text());
  }

  @Test
  void theDigestComparesTheWeekAndNamesTheTopRescuer() {
    MetricsCalculator calculator = new MetricsCalculator(FreshnessPolicy.DEFAULT);
    Map<String, BigDecimal> prices = Map.of("chicken breast", new BigDecimal("22000"));
    HouseholdMetrics lastWeek =
        calculator.calculate(
            List.of(
                move(JUAN, MovementType.CONSUME, 500, "rice", MONDAY.minusDays(5), null),
                move(JUAN, MovementType.DISCARD, 500, "bread", MONDAY.minusDays(5), null)),
            prices,
            ZoneOffset.UTC,
            MONDAY.minusDays(7),
            MONDAY.minusDays(1));
    HouseholdMetrics thisWeek =
        calculator.calculate(
            List.of(
                move(ANA, MovementType.CONSUME, 500, "chicken breast", MONDAY, MONDAY.plusDays(1)),
                move(JUAN, MovementType.CONSUME, 400, "rice", MONDAY, null),
                move(JUAN, MovementType.DISCARD, 100, "bread", MONDAY, null)),
            prices,
            ZoneOffset.UTC,
            MONDAY,
            MONDAY.plusDays(6));

    WeeklyDigest digest =
        new WeeklyDigest(
            thisWeek, lastWeek, List.of(), Currency.getInstance("COP"), Map.of(ANA, "Ana"));

    assertEquals(
        "This week you rescued 500 g before it expired and saved 11000.00 COP. You threw away"
            + " 100 g (10% of what left the fridge, 40 points better than last week). Top rescuer:"
            + " Ana.\nNo food was thrown away twice; keep using what expires first.",
        digest.text());
    WeeklyDigest same =
        new WeeklyDigest(lastWeek, lastWeek, List.of(), Currency.getInstance("COP"), Map.of());
    assertTrue(same.text().contains("the same as last week"));
    assertTrue(same.topRescuer().isEmpty());
    WeeklyDigest worse =
        new WeeklyDigest(
            lastWeek,
            thisWeek,
            List.of(new WasteTip("bread", 2, Grams.of(500), 50)),
            Currency.getInstance("COP"),
            Map.of());
    assertTrue(worse.text().contains("40 points worse than last week"));
    assertTrue(worse.text().endsWith("plan a dish for it earlier."));
    WeeklyDigest unnamed =
        new WeeklyDigest(thisWeek, lastWeek, List.of(), Currency.getInstance("COP"), Map.of());
    assertEquals("A former member", unnamed.topRescuer().orElseThrow());
  }
}
