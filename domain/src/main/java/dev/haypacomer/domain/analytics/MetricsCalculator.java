package dev.haypacomer.domain.analytics;

import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementType;
import dev.haypacomer.domain.quantity.Grams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class MetricsCalculator {

  private static final BigDecimal GRAMS_PER_KILO = BigDecimal.valueOf(1000);

  private final FreshnessPolicy freshness;

  public MetricsCalculator(FreshnessPolicy freshness) {
    this.freshness = freshness;
  }

  public HouseholdMetrics calculate(
      List<InventoryMovement> movements,
      Map<String, BigDecimal> pricePerKg,
      ZoneId zone,
      LocalDate from,
      LocalDate to) {
    if (to.isBefore(from)) {
      throw new IllegalArgumentException("The period ends before it starts");
    }
    Map<String, Tally> foods = new HashMap<>();
    Map<UserId, Tally> members = new HashMap<>();
    Map<LocalDate, Tally> days = new TreeMap<>();
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      days.put(day, Tally.EMPTY);
    }
    Tally total = Tally.EMPTY;
    BigDecimal saved = BigDecimal.ZERO;
    BigDecimal wasted = BigDecimal.ZERO;
    Set<String> unpriced = new TreeSet<>();
    for (InventoryMovement movement : movements) {
      LocalDate day = LocalDate.ofInstant(movement.at(), zone);
      Optional<Tally> counted = tally(movement, day);
      if (counted.isEmpty() || day.isBefore(from) || day.isAfter(to)) {
        continue;
      }
      Tally tally = counted.get();
      String food = movement.food().orElse("unknown");
      total = total.plus(tally);
      foods.merge(food, tally, Tally::plus);
      members.merge(movement.actor(), tally, Tally::plus);
      days.merge(day, tally, Tally::plus);
      BigDecimal price = pricePerKg.get(food);
      if (price == null) {
        if (!tally.rescued().isZero() || !tally.discarded().isZero()) {
          unpriced.add(food);
        }
        continue;
      }
      saved = saved.add(money(tally.rescued(), price));
      wasted = wasted.add(money(tally.discarded(), price));
    }
    return new HouseholdMetrics(
        from,
        to,
        total,
        saved.setScale(2, RoundingMode.HALF_UP),
        wasted.setScale(2, RoundingMode.HALF_UP),
        foods.entrySet().stream()
            .map(entry -> new FoodTally(entry.getKey(), entry.getValue()))
            .sorted(
                Comparator.comparing((FoodTally food) -> food.tally().consumed())
                    .reversed()
                    .thenComparing(FoodTally::foodKey))
            .toList(),
        members.entrySet().stream()
            .map(entry -> new MemberTally(entry.getKey(), entry.getValue()))
            .sorted(
                Comparator.comparing((MemberTally member) -> member.tally().rescued())
                    .reversed()
                    .thenComparing(member -> member.user().value()))
            .toList(),
        days.entrySet().stream()
            .map(entry -> new DayTally(entry.getKey(), entry.getValue()))
            .toList(),
        unpriced);
  }

  private Optional<Tally> tally(InventoryMovement movement, LocalDate day) {
    Grams grams = Grams.of(movement.deltaGrams().abs());
    if (movement.type() == MovementType.CONSUME) {
      boolean rescued =
          movement
              .expiry()
              .map(
                  expiry ->
                      !expiry.isBefore(day)
                          && !expiry.isAfter(day.plusDays(freshness.atRiskDays())))
              .orElse(false);
      return Optional.of(new Tally(grams, rescued ? grams : Grams.ZERO, Grams.ZERO));
    }
    if (movement.type() == MovementType.DISCARD) {
      return Optional.of(new Tally(Grams.ZERO, Grams.ZERO, grams));
    }
    return Optional.empty();
  }

  private static BigDecimal money(Grams grams, BigDecimal pricePerKg) {
    return grams.value().multiply(pricePerKg).divide(GRAMS_PER_KILO, 4, RoundingMode.HALF_UP);
  }
}
