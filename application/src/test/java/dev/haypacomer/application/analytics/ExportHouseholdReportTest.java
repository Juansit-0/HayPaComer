package dev.haypacomer.application.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.household.HouseholdNotFoundException;
import dev.haypacomer.application.port.FoodPriceRepository;
import dev.haypacomer.application.port.UserRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.application.support.InMemoryInventoryStores;
import dev.haypacomer.domain.fridge.FoodItemId;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.EmailAddress;
import dev.haypacomer.domain.identity.PasswordHash;
import dev.haypacomer.domain.identity.User;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.inventory.FreshnessPolicy;
import dev.haypacomer.domain.inventory.InventoryMovement;
import dev.haypacomer.domain.inventory.MovementSource;
import dev.haypacomer.domain.inventory.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExportHouseholdReportTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 9);

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final InMemoryInventoryStores stores = new InMemoryInventoryStores();
  private final UserId owner = UserId.newId();
  private final UserId ana = UserId.newId();
  private final Household home =
      Household.create(
          "Apartment", Currency.getInstance("COP"), ZoneOffset.UTC, owner, Instant.now());
  private final UserRepository users =
      new UserRepository() {
        @Override
        public void save(User user) {}

        @Override
        public Optional<User> findById(UserId id) {
          return id.equals(owner) ? Optional.of(user(owner, "Juan")) : Optional.empty();
        }

        @Override
        public Optional<User> findByEmail(EmailAddress email) {
          return Optional.empty();
        }
      };
  private final FoodPriceRepository prices =
      new FoodPriceRepository() {
        @Override
        public Map<String, BigDecimal> pricesFor(HouseholdId household, Currency currency) {
          return Map.of("rice", new BigDecimal("4800"));
        }

        @Override
        public void save(HouseholdId household, String foodKey, BigDecimal pricePerKg) {}
      };
  private final ExportHouseholdReport export =
      new ExportHouseholdReport(
          households,
          users,
          new ViewHouseholdMetrics(households, stores.history, prices, FreshnessPolicy.DEFAULT));

  {
    home.join(ana, Role.MEMBER, Instant.now());
    households.save(home);
    stores.movements.record(
        new InventoryMovement(
            UUID.randomUUID(),
            home.id(),
            FoodItemId.newId(),
            owner,
            MovementType.DISCARD,
            new BigDecimal("-500"),
            MovementSource.MANUAL,
            DAY.atTime(10, 0).toInstant(ZoneOffset.UTC),
            "rice",
            null));
  }

  private static User user(UserId id, String name) {
    return new User(
        id,
        new EmailAddress("juan@haypacomer.dev"),
        new PasswordHash("$2a$12$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyz01234"),
        name,
        true,
        true,
        Instant.now());
  }

  @Test
  void exportsTheSameMetricsAsCsvOrMarkdownWithMemberNames() {
    ExportedReport csv = export.export(ana, home.id(), DAY, DAY, ReportFormat.CSV);
    ExportedReport markdown = export.export(ana, home.id(), DAY, DAY, ReportFormat.MARKDOWN);

    assertEquals("haypacomer-2026-10-09-2026-10-09.csv", csv.fileName());
    assertEquals("text/csv", csv.mediaType());
    assertTrue(csv.content().contains("member,Juan,0,0,500,1.000"));
    assertTrue(csv.content().contains("money,wasted_COP,2400.00,,,"));
    assertEquals("text/markdown", markdown.mediaType());
    assertTrue(markdown.content().startsWith("# Apartment kitchen report"));
    assertTrue(markdown.content().contains("| Juan | 0.00 kg | 0.00 kg | 0.50 kg |"));
    assertThrows(
        HouseholdNotFoundException.class,
        () -> export.export(UserId.newId(), home.id(), DAY, DAY, ReportFormat.CSV));
  }
}
