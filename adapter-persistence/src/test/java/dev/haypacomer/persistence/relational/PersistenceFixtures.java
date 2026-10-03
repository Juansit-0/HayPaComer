package dev.haypacomer.persistence.relational;

import dev.haypacomer.domain.food.Allergen;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.PasswordHash;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Instant;
import java.util.Set;

final class PersistenceFixtures {

  static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

  static final FoodMetadata MILK =
      new FoodMetadata(
          "Milk",
          FoodCategory.DAIRY,
          Unit.MILLILITER,
          ConversionFactors.withDensity("1.03"),
          true,
          7,
          Set.of(Allergen.MILK));

  static final FoodMetadata EGG =
      new FoodMetadata(
          "Egg",
          FoodCategory.EGGS,
          Unit.PIECE,
          ConversionFactors.withPieceWeight("50"),
          true,
          21,
          Set.of(Allergen.EGGS));

  static final FoodMetadata CHICKEN =
      new FoodMetadata(
          "Chicken breast",
          FoodCategory.POULTRY,
          Unit.GRAM,
          ConversionFactors.MASS_ONLY,
          true,
          2,
          Set.of());

  private PersistenceFixtures() {}

  static User user(String email, String name) {
    return User.register(new EmailAddress(email), new PasswordHash("$2a$12$hash"), name, NOW);
  }
}
