package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.port.HouseholdDirectory;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Membership;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import dev.haypacomer.domain.member.MemberId;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PostgresHouseholdRepository implements HouseholdRepository, HouseholdDirectory {

  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public PostgresHouseholdRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
    this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Override
  public void save(Household household) {
    transaction.executeWithoutResult(status -> write(household));
  }

  @Override
  public Optional<Household> findById(HouseholdId id) {
    return jdbc.sql("SELECT id, name, currency, timezone FROM households WHERE id = :id")
        .param("id", id.value())
        .query(
            (row, rowNumber) ->
                new HouseholdRow(
                    row.getObject("id", UUID.class),
                    row.getString("name"),
                    row.getString("currency"),
                    row.getString("timezone")))
        .optional()
        .map(this::restore);
  }

  @Override
  public List<HouseholdId> all() {
    return jdbc
        .sql("SELECT id FROM households ORDER BY created_at, id")
        .query(UUID.class)
        .list()
        .stream()
        .map(HouseholdId::new)
        .toList();
  }

  @Override
  public List<Household> findByUser(UserId user) {
    return jdbc
        .sql(
            """
            SELECT h.id FROM households h
            JOIN household_members m ON m.household_id = h.id
            WHERE m.user_id = :user
            ORDER BY h.created_at, h.id
            """)
        .param("user", user.value())
        .query(UUID.class)
        .list()
        .stream()
        .map(id -> findById(new HouseholdId(id)).orElseThrow())
        .toList();
  }

  private void write(Household household) {
    UUID householdId = household.id().value();
    jdbc.sql(
            """
            INSERT INTO households (id, name, currency, timezone)
            VALUES (:id, :name, :currency, :timezone)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                currency = EXCLUDED.currency,
                timezone = EXCLUDED.timezone
            """)
        .param("id", householdId)
        .param("name", household.name())
        .param("currency", household.currency().getCurrencyCode())
        .param("timezone", household.timezone().getId())
        .update();
    Set<UUID> current = new HashSet<>();
    household.memberships().forEach(membership -> current.add(membership.user().value()));
    jdbc
        .sql("SELECT user_id FROM household_members WHERE household_id = :id")
        .param("id", householdId)
        .query(UUID.class)
        .list()
        .stream()
        .filter(user -> !current.contains(user))
        .forEach(
            user ->
                jdbc.sql(
                        "DELETE FROM household_members WHERE household_id = :id AND user_id = :user")
                    .param("id", householdId)
                    .param("user", user)
                    .update());
    household.memberships().stream()
        .sorted(Comparator.comparing(membership -> membership.role() == Role.OWNER))
        .forEach(membership -> upsertMembership(householdId, membership));
  }

  private void upsertMembership(UUID householdId, Membership membership) {
    jdbc.sql(
            """
            INSERT INTO household_members (household_id, user_id, member_id, role, joined_at)
            VALUES (:household, :user, :member, :role, :joinedAt)
            ON CONFLICT (household_id, user_id) DO UPDATE SET role = EXCLUDED.role
            """)
        .param("household", householdId)
        .param("user", membership.user().value())
        .param("member", membership.member().value())
        .param("role", membership.role().name())
        .param("joinedAt", Timestamps.toDatabase(membership.joinedAt()))
        .update();
  }

  private Household restore(HouseholdRow row) {
    List<Membership> memberships =
        jdbc.sql(
                """
                SELECT user_id, member_id, role, joined_at FROM household_members
                WHERE household_id = :id ORDER BY joined_at, user_id
                """)
            .param("id", row.id())
            .query(
                (result, rowNumber) ->
                    new Membership(
                        new UserId(result.getObject("user_id", UUID.class)),
                        new MemberId(result.getObject("member_id", UUID.class)),
                        Role.valueOf(result.getString("role")),
                        Timestamps.read(result, "joined_at")))
            .list();
    return Household.restore(
        new HouseholdId(row.id()),
        row.name(),
        Currency.getInstance(row.currency()),
        ZoneId.of(row.timezone()),
        memberships);
  }

  private record HouseholdRow(UUID id, String name, String currency, String timezone) {}
}
