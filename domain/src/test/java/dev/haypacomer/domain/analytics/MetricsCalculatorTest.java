package dev.haypacomer.domain.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MetricsCalculatorTest {

  private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
  private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
  private static final HouseholdId HOME = HouseholdId.newId();
  private static final UserId JUAN = UserId.newId();
  private static final UserId ANA = UserId.newId();

  private final MetricsCalculator calculator = new MetricsCalculator(FreshnessPolicy.DEFAULT);

  private static Instant noonOf(LocalDate day) {
    return day.atTime(12, 0).atZone(BOGOTA).toInstant();
  }

  private static InventoryMovement move(
      UserId actor, MovementType type, long grams, String food, LocalDate day, LocalDate expiry) {
    return new InventoryMovement(
        UUID.randomUUID(),
        HOME,
        FoodItemId.newId(),
        actor,
        type,
        BigDecimal.valueOf(type == MovementType.ADD ? grams : -grams),
        MovementSource.SCALE,
        noonOf(day),
        food,
        expiry);
  }

  @Test
  void countsRescuedFoodWasteAndMoneyPerFoodMemberAndDay() {
    List<InventoryMovement> movements =
        List.of(
            move(JUAN, MovementType.ADD, 1000, "chicken breast", MONDAY, MONDAY.plusDays(2)),
            move(JUAN, MovementType.CONSUME, 400, "chicken breast", MONDAY, MONDAY.plusDays(2)),
            move(
                ANA,
                MovementType.CONSUME,
                200,
                "chicken breast",
                MONDAY.plusDays(1),
                MONDAY.plusDays(2)),
            move(ANA, MovementType.CONSUME, 300, "rice", MONDAY.plusDays(1), null),
            move(ANA, MovementType.CONSUME, 100, "milk", MONDAY.plusDays(1), MONDAY.plusDays(9)),
            move(JUAN, MovementType.DISCARD, 250, "yogurt", MONDAY.plusDays(2), MONDAY),
            move(JUAN, MovementType.CONSUME, 50, "yogurt", MONDAY.plusDays(2), MONDAY),
            move(JUAN, MovementType.DISCARD, 80, "mystery", MONDAY.plusDays(2), null),
            move(JUAN, MovementType.CONSUME, 999, "chicken breast", MONDAY.minusDays(1), MONDAY));
    Map<String, BigDecimal> prices =
        Map.of(
            "chicken breast", new BigDecimal("22000"),
            "rice", new BigDecimal("4800"),
            "yogurt", new BigDecimal("14000"));

    HouseholdMetrics metrics =
        calculator.calculate(movements, prices, BOGOTA, MONDAY, MONDAY.plusDays(6));

    assertEquals(Grams.of(1050), metrics.total().consumed());
    assertEquals(Grams.of(600), metrics.total().rescued());
    assertEquals(Grams.of(330), metrics.total().discarded());
    assertEquals(new BigDecimal("13200.00"), metrics.moneySaved());
    assertEquals(new BigDecimal("3500.00"), metrics.moneyWasted());
    assertEquals(Set.of("mystery"), metrics.unpriced());
    assertEquals("chicken breast", metrics.foods().getFirst().foodKey());
    assertEquals(7, metrics.days().size());
    assertEquals(Grams.of(400), metrics.days().getFirst().tally().rescued());
    assertEquals(Grams.of(330), metrics.days().get(2).tally().discarded());
    assertEquals(JUAN, metrics.members().getFirst().user());
    assertEquals(330.0 / 1380.0, metrics.total().wasteRate(), 1e-9);
  }

  @Test
  void anEmptyPeriodIsZeroEverywhere() {
    HouseholdMetrics metrics = calculator.calculate(List.of(), Map.of(), BOGOTA, MONDAY, MONDAY);

    assertEquals(Tally.EMPTY, metrics.total());
    assertEquals(0.0, metrics.total().wasteRate());
    assertEquals(new BigDecimal("0.00"), metrics.moneySaved());
    assertTrue(metrics.foods().isEmpty());
    assertEquals(1, metrics.days().size());
    assertThrows(
        IllegalArgumentException.class,
        () -> calculator.calculate(List.of(), Map.of(), BOGOTA, MONDAY, MONDAY.minusDays(1)));
  }

  @Test
  void legacyMovementsWithoutFoodStillCount() {
    InventoryMovement legacy =
        new InventoryMovement(
            UUID.randomUUID(),
            HOME,
            FoodItemId.newId(),
            JUAN,
            MovementType.DISCARD,
            new BigDecimal("-40"),
            MovementSource.MANUAL,
            noonOf(MONDAY));

    HouseholdMetrics metrics =
        calculator.calculate(List.of(legacy), Map.of(), BOGOTA, MONDAY, MONDAY);

    assertEquals("unknown", metrics.foods().getFirst().foodKey());
    assertEquals(Set.of("unknown"), metrics.unpriced());
    assertTrue(legacy.food().isEmpty());
    assertTrue(legacy.expiry().isEmpty());
  }
}
