package dev.haypacomer.application.ai;

import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.AiRateLimiter;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.application.port.RecipePhotoReader;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.Grams;
import dev.haypacomer.domain.quantity.QuantityParser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ReadRecipePhoto {

  private final GetHousehold households;
  private final RecipePhotoReader reader;
  private final FoodCatalogRepository catalog;
  private final AiRateLimiter rateLimiter;
  private final AiAuditLog audit;
  private final PolicySource policies;
  private final Clock clock;

  public ReadRecipePhoto(
      HouseholdRepository households,
      RecipePhotoReader reader,
      FoodCatalogRepository catalog,
      AiRateLimiter rateLimiter,
      AiAuditLog audit,
      Clock clock) {
    this(households, reader, catalog, rateLimiter, audit, FixedPolicies.DEFAULT, clock);
  }

  public ReadRecipePhoto(
      HouseholdRepository households,
      RecipePhotoReader reader,
      FoodCatalogRepository catalog,
      AiRateLimiter rateLimiter,
      AiAuditLog audit,
      PolicySource policies,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.reader = Objects.requireNonNull(reader, "reader");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    this.audit = Objects.requireNonNull(audit, "audit");
    this.policies = Objects.requireNonNull(policies, "policies");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public RecipeDraft read(UserId actor, HouseholdId household, RecipePhoto photo) {
    households.get(actor, household);
    if (!rateLimiter.tryAcquire(
        actor.value().toString(), policies.aiCallsPerMinute(), Duration.ofMinutes(1))) {
      throw new AiRateLimitExceededException();
    }
    Instant started = clock.instant();
    PhotoRecipe read;
    try {
      read = reader.read(photo);
    } catch (PhotoReadingUnavailableException unavailable) {
      record(started, AiOutcome.UNAVAILABLE);
      throw unavailable;
    }
    record(started, AiOutcome.VALID);
    return new RecipeDraft(
        read.name(),
        read.servings(),
        read.minutes(),
        read.ingredients().stream().map(this::verify).toList(),
        read.steps(),
        read.confidence(),
        read.source());
  }

  private DraftIngredient verify(PhotoIngredient ingredient) {
    Optional<FoodMetadata> food = catalog.findByName(ingredient.food());
    if (food.isEmpty()) {
      return new DraftIngredient(
          ingredient.food(), ingredient.quantity(), null, null, "Not in the food catalog");
    }
    try {
      Grams grams = QuantityParser.parse(ingredient.quantity()).interpret(food.get().conversion());
      return new DraftIngredient(
          ingredient.food(), ingredient.quantity(), food.get().name(), grams, null);
    } catch (RuntimeException unreadable) {
      return new DraftIngredient(
          ingredient.food(),
          ingredient.quantity(),
          food.get().name(),
          null,
          "Quantity cannot be weighed: " + unreadable.getMessage());
    }
  }

  private void record(Instant started, AiOutcome outcome) {
    Duration latency = Duration.between(started, clock.instant());
    audit.record(
        new AiAuditEntry(
            reader.provider(),
            "readRecipePhoto",
            latency.isNegative() ? Duration.ZERO : latency,
            outcome,
            clock.instant()));
  }
}
