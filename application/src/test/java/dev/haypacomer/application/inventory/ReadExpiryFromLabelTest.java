package dev.haypacomer.application.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.haypacomer.application.agent.AiAuditEntry;
import dev.haypacomer.application.agent.AiOutcome;
import dev.haypacomer.application.ai.AdvisorSource;
import dev.haypacomer.application.ai.AiRateLimitExceededException;
import dev.haypacomer.application.ai.LabelReading;
import dev.haypacomer.application.ai.PhotoReadingUnavailableException;
import dev.haypacomer.application.ai.RecipePhoto;
import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.port.AiAuditLog;
import dev.haypacomer.application.port.LabelPhotoReader;
import dev.haypacomer.application.settings.FixedPolicies;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.expiry.ExpirySource;
import dev.haypacomer.domain.expiry.ShelfLife;
import dev.haypacomer.domain.food.FoodCategory;
import dev.haypacomer.domain.food.FoodMetadata;
import dev.haypacomer.domain.fridge.ZoneKind;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.quantity.ConversionFactors;
import dev.haypacomer.domain.quantity.Unit;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReadExpiryFromLabelTest {

  private static final Instant NOW = Instant.parse("2026-10-09T15:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);
  private static final RecipePhoto PHOTO =
      new RecipePhoto("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1});

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final List<AiAuditEntry> audited = new ArrayList<>();
  private final UserId juan = UserId.newId();
  private final Household home =
      Household.create("Home", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
  private Supplier<LabelReading> answer;
  private boolean allowed = true;

  private final AiAuditLog audit =
      new AiAuditLog() {
        @Override
        public void record(AiAuditEntry entry) {
          audited.add(entry);
        }

        @Override
        public List<AiAuditEntry> recent(int limit) {
          return audited;
        }
      };

  private final LabelPhotoReader reader =
      new LabelPhotoReader() {
        @Override
        public String provider() {
          return "scripted";
        }

        @Override
        public LabelReading read(RecipePhoto photo) {
          return answer.get();
        }
      };

  private final ReadExpiryFromLabel read =
      new ReadExpiryFromLabel(
          households,
          reader,
          stores.catalog,
          ExpiryDesk.enforcing(food -> new ShelfLife(7, 5, 90, 4), FixedPolicies.DEFAULT),
          (subject, limit, window) -> allowed,
          audit,
          FixedPolicies.DEFAULT,
          Clock.fixed(NOW, ZoneOffset.UTC));

  ReadExpiryFromLabelTest() {
    households.save(home);
    stores.catalog.save(
        new FoodMetadata(
            "Milk", FoodCategory.DAIRY, Unit.GRAM, ConversionFactors.MASS_ONLY, true, 7, Set.of()));
  }

  private ExpiryProposal label(LocalDate date, double confidence) {
    answer = () -> new LabelReading("Leche entera", date, confidence, AdvisorSource.GEMINI);
    return read.read(juan, home.id(), "Milk", ZoneKind.SHELF, false, PHOTO);
  }

  @Test
  void aReadableAndPossibleDateIsProposedFromTheLabel() {
    ExpiryProposal proposal = label(TODAY.plusDays(6), 0.95);

    assertEquals("Milk", proposal.food());
    assertEquals("Leche entera", proposal.productOnLabel());
    assertEquals(TODAY.plusDays(6), proposal.expiresOn());
    assertEquals(ExpirySource.LABEL, proposal.source());
    assertEquals(0.9, proposal.confidence());
    assertEquals("Read from the label", proposal.reason());
    assertEquals(AiOutcome.VALID, audited.getLast().outcome());
    assertEquals("readExpiryLabel", audited.getLast().operation());
  }

  @Test
  void impossibleUnreadableOrUnsureDatesFallBackToTheEstimate() {
    ExpiryProposal impossible = label(TODAY.plusDays(40), 0.95);
    assertEquals(ExpirySource.ESTIMATED, impossible.source());
    assertEquals(TODAY.plusDays(7), impossible.expiresOn());
    assertEquals(TODAY.plusDays(40), impossible.printedDate());
    assertEquals(
        "Milk lasts about 7 days in the fridge, so a date 40 days away is not possible;"
            + " check the label or let HayPaComer estimate it",
        impossible.reason());

    assertEquals(ReadExpiryFromLabel.NO_DATE, label(null, 0.9).reason());
    assertEquals(ReadExpiryFromLabel.UNSURE, label(TODAY.plusDays(3), 0.3).reason());
    assertEquals("The expiry date is in the past", label(TODAY.minusDays(1), 0.9).reason());
  }

  @Test
  void anOutageStillGivesTheUsualShelfLife() {
    answer =
        () -> {
          throw PhotoReadingUnavailableException.providerDown("down");
        };
    ExpiryProposal down = read.read(juan, home.id(), "Milk", ZoneKind.DOOR, false, PHOTO);
    assertEquals(TODAY.plusDays(5), down.expiresOn());
    assertEquals(ReadExpiryFromLabel.NO_AI, down.reason());
    assertNull(down.productOnLabel());
    assertEquals(AiOutcome.UNAVAILABLE, audited.getLast().outcome());

    answer =
        () -> {
          throw new PhotoReadingUnavailableException("not a label");
        };
    assertEquals(
        ReadExpiryFromLabel.UNREADABLE,
        read.read(juan, home.id(), "Milk", ZoneKind.SHELF, true, PHOTO).reason());
  }

  @Test
  void membersFoodsAndTheRateLimitAreChecked() {
    assertThrows(
        HouseholdNotFoundException.class,
        () -> read.read(UserId.newId(), home.id(), "Milk", ZoneKind.SHELF, false, PHOTO));
    assertThrows(
        FoodNotInCatalogException.class,
        () -> read.read(juan, home.id(), "Unicorn", ZoneKind.SHELF, false, PHOTO));
    allowed = false;
    assertThrows(AiRateLimitExceededException.class, () -> label(TODAY, 0.9));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LabelReading("x", null, 1.5, AdvisorSource.GEMINI));
  }
}
