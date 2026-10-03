package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.audit.AuditEntry;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresAuditAndUnitOfWorkTest extends PostgresTestSupport {

  private User juan;
  private Household household;

  @BeforeEach
  void createData() {
    juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
  }

  @Test
  void recordsAndReadsRecentEntriesWithDetail() {
    PostgresAuditLog audit = new PostgresAuditLog(dataSource);
    UUID item = UUID.randomUUID();
    AuditEntry stock =
        new AuditEntry(
            juan.id(),
            household.id(),
            "STOCK_FOOD",
            "FOOD_ITEM",
            item,
            Map.of("food", "Milk"),
            NOW);
    AuditEntry consume =
        new AuditEntry(
            juan.id(),
            household.id(),
            "CONSUME_FOOD",
            "FOOD_ITEM",
            item,
            Map.of("grams", "192.00", "note", "it's \"quoted\""),
            NOW.plusSeconds(60));

    audit.record(stock);
    audit.record(consume);

    assertEquals(List.of(consume, stock), audit.recent(household.id(), 10));
    assertEquals(List.of(consume), audit.recent(household.id(), 1));
  }

  @Test
  void unitOfWorkRollsBackEveryWriteOnFailure() {
    PostgresUnitOfWork unitOfWork = new PostgresUnitOfWork(dataSource);
    PostgresUserRepository users = new PostgresUserRepository(dataSource);
    User ana = user("ana@haypacomer.dev", "Ana");

    assertThrows(
        IllegalStateException.class,
        () ->
            unitOfWork.run(
                () -> {
                  users.save(ana);
                  throw new IllegalStateException("boom");
                }));

    assertTrue(users.findById(ana.id()).isEmpty());
    assertEquals("ok", unitOfWork.run(() -> "ok"));
  }
}
