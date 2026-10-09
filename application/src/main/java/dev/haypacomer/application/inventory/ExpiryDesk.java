package dev.haypacomer.application.inventory;

import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.ShelfLifeCatalog;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.expiry.ShelfLife;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.HouseholdId;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record ExpiryDesk(ShelfLifeCatalog shelfLives, PolicySource policies, boolean enforced) {

  public static final ExpiryDesk LENIENT =
      new ExpiryDesk(ShelfLife::of, FixedPolicies.DEFAULT, false);

  public ExpiryDesk {
    Objects.requireNonNull(shelfLives, "shelfLives");
    Objects.requireNonNull(policies, "policies");
  }

  public static ExpiryDesk enforcing(ShelfLifeCatalog shelfLives, PolicySource policies) {
    return new ExpiryDesk(shelfLives, policies, true);
  }

  public Optional<ExpiryEstimate> resolve(
      HouseholdId household,
      FoodMetadata food,
      ZoneKind zone,
      boolean opened,
      LocalDate date,
      ExpirySource source,
      LocalDate today) {
    ExpirySource given = source == null ? ExpirySource.USER : source;
    if (!enforced) {
      return Optional.ofNullable(date)
          .map(value -> new ExpiryEstimate(value, given, food.shelfDays()));
    }
    ShelfLife life = shelfLives.shelfLife(food);
    if (date == null) {
      return Optional.of(policies.expiryRules(household).estimate(life, zone, opened, today));
    }
    return Optional.of(
        policies.expiryRules(household).check(food, life, zone, opened, date, given, today));
  }

  public ExpiryEstimate estimate(
      HouseholdId household, FoodMetadata food, ZoneKind zone, boolean opened, LocalDate today) {
    return policies
        .expiryRules(household)
        .estimate(shelfLives.shelfLife(food), zone, opened, today);
  }

  public LocalDate afterOpening(
      HouseholdId household, FoodMetadata food, ZoneKind zone, LocalDate current, LocalDate today) {
    return policies
        .expiryRules(household)
        .afterOpening(shelfLives.shelfLife(food), zone, current, today);
  }
}
