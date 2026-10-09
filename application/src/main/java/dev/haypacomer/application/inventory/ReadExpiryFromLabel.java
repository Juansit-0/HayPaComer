package dev.haypacomer.application.inventory;

import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.ai.AiRateLimitExceededException;
import dev.haypacomer.application.ai.LabelReading;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.household.GetHousehold;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.AiRateLimiter;
import dev.haypacomer.application.port.FoodCatalogRepository;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.LabelPhotoReader;
import dev.haypacomer.application.port.PolicySource;
import dev.haypacomer.domain.expiry.ExpiryEstimate;
import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.expiry.ImpossibleExpiryException;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public final class ReadExpiryFromLabel {

  public static final double MIN_CONFIDENCE = 0.6;
  static final String FROM_LABEL = "Read from the label";
  static final String NO_DATE = "The label has no readable date; this is the usual shelf life";
  static final String UNSURE =
      "The label could not be read with confidence; this is the usual shelf life";
  static final String NO_AI = "The AI is not available; this is the usual shelf life";
  static final String UNREADABLE = "The label could not be read; this is the usual shelf life";

  private final GetHousehold households;
  private final LabelPhotoReader reader;
  private final FoodCatalogRepository catalog;
  private final ExpiryDesk expiry;
  private final AiRateLimiter rateLimiter;
  private final AiAuditLog audit;
  private final PolicySource policies;
  private final Clock clock;

  public ReadExpiryFromLabel(
      HouseholdRepository households,
      LabelPhotoReader reader,
      FoodCatalogRepository catalog,
      ExpiryDesk expiry,
      AiRateLimiter rateLimiter,
      AiAuditLog audit,
      PolicySource policies,
      Clock clock) {
    this.households = new GetHousehold(households);
    this.reader = Objects.requireNonNull(reader, "reader");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.expiry = Objects.requireNonNull(expiry, "expiry");
    this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    this.audit = Objects.requireNonNull(audit, "audit");
    this.policies = Objects.requireNonNull(policies, "policies");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public ExpiryProposal read(
      UserId actor,
      HouseholdId householdId,
      String foodName,
      ZoneKind zone,
      boolean opened,
      RecipePhoto photo) {
    Household household = households.get(actor, householdId);
    FoodMetadata food =
        catalog.findByName(foodName).orElseThrow(() -> new FoodNotInCatalogException(foodName));
    LocalDate today = LocalDate.ofInstant(clock.instant(), household.timezone());
    ExpiryEstimate usual = expiry.estimate(householdId, food, zone, opened, today);
    if (!rateLimiter.tryAcquire(
        actor.value().toString(), policies.aiCallsPerMinute(), Duration.ofMinutes(1))) {
      throw new AiRateLimitExceededException();
    }
    Instant started = clock.instant();
    LabelReading label;
    try {
      label = reader.read(photo);
    } catch (PhotoReadingUnavailableException unavailable) {
      record(started, AiOutcome.UNAVAILABLE);
      return estimated(food, null, usual, unavailable.isProviderDown() ? NO_AI : UNREADABLE);
    }
    record(started, AiOutcome.VALID);
    if (label.date().isEmpty()) {
      return estimated(food, label, usual, NO_DATE);
    }
    if (label.confidence() < MIN_CONFIDENCE) {
      return estimated(food, label, usual, UNSURE);
    }
    try {
      ExpiryEstimate read =
          expiry
              .resolve(
                  householdId, food, zone, opened, label.printedDate(), ExpirySource.LABEL, today)
              .orElseThrow();
      return new ExpiryProposal(
          food.name(),
          label.product(),
          label.printedDate(),
          read.date(),
          ExpirySource.LABEL,
          Math.min(label.confidence(), ExpirySource.LABEL.confidence()),
          FROM_LABEL);
    } catch (ImpossibleExpiryException impossible) {
      return estimated(food, label, usual, impossible.getMessage());
    }
  }

  private static ExpiryProposal estimated(
      FoodMetadata food, LabelReading label, ExpiryEstimate usual, String reason) {
    return new ExpiryProposal(
        food.name(),
        label == null ? null : label.product(),
        label == null ? null : label.printedDate(),
        usual.date(),
        ExpirySource.ESTIMATED,
        usual.confidence(),
        reason);
  }

  private void record(Instant started, AiOutcome outcome) {
    Duration latency = Duration.between(started, clock.instant());
    audit.record(
        new AiAuditEntry(
            reader.provider(),
            "readExpiryLabel",
            latency.isNegative() ? Duration.ZERO : latency,
            outcome,
            clock.instant()));
  }
}
