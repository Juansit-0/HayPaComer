package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.inventory.InventorySnapshot;
import dev.haypacomer.application.inventory.SnapshotKind;
import dev.haypacomer.application.port.SnapshotStore;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresSnapshotStore implements SnapshotStore {

  private static final String SELECT =
      "SELECT id, household_id, kind, command_id, actor_user_id, reason, payload::text AS payload,"
          + " at, used_at FROM inventory_snapshots";

  private final JdbcClient jdbc;

  public PostgresSnapshotStore(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void save(InventorySnapshot snapshot) {
    jdbc.sql(
            """
            INSERT INTO inventory_snapshots (id, household_id, kind, command_id, actor_user_id,
                                             reason, payload, at, used_at)
            VALUES (:id, :household, :kind, :command, (SELECT id FROM users WHERE id = :actor),
                    :reason, :payload::jsonb, :at, :usedAt)
            ON CONFLICT (id) DO UPDATE SET used_at = EXCLUDED.used_at
            """)
        .param("id", snapshot.id())
        .param("household", snapshot.household().value())
        .param("kind", snapshot.kind().name())
        .param("command", snapshot.commandId(), Types.OTHER)
        .param("actor", snapshot.actor().value())
        .param("reason", snapshot.reason())
        .param("payload", SnapshotPayload.write(snapshot.memento()))
        .param("at", Timestamps.toDatabase(snapshot.at()))
        .param(
            "usedAt",
            snapshot.usedAt() == null ? null : Timestamps.toDatabase(snapshot.usedAt()),
            Types.TIMESTAMP_WITH_TIMEZONE)
        .update();
  }

  @Override
  public Optional<InventorySnapshot> find(HouseholdId household, UUID id) {
    return jdbc.sql(SELECT + " WHERE household_id = :household AND id = :id")
        .param("household", household.value())
        .param("id", id)
        .query(this::map)
        .optional();
  }

  @Override
  public Optional<InventorySnapshot> latestUnusedUndo(HouseholdId household) {
    return jdbc.sql(
            SELECT
                + " WHERE household_id = :household AND kind = 'UNDO' AND used_at IS NULL"
                + " ORDER BY seq DESC LIMIT 1")
        .param("household", household.value())
        .query(this::map)
        .optional();
  }

  @Override
  public List<InventorySnapshot> list(HouseholdId household, SnapshotKind kind, int limit) {
    return jdbc.sql(
            SELECT
                + " WHERE household_id = :household AND kind = :kind ORDER BY seq DESC LIMIT :limit")
        .param("household", household.value())
        .param("kind", kind.name())
        .param("limit", limit)
        .query(this::map)
        .list();
  }

  @Override
  public void markUsed(UUID id, Instant at) {
    jdbc.sql("UPDATE inventory_snapshots SET used_at = :at WHERE id = :id")
        .param("at", Timestamps.toDatabase(at))
        .param("id", id)
        .update();
  }

  @Override
  public void closeUndoHistory(HouseholdId household, Instant at) {
    jdbc.sql(
            "UPDATE inventory_snapshots SET used_at = :at"
                + " WHERE household_id = :household AND kind = 'UNDO' AND used_at IS NULL")
        .param("at", Timestamps.toDatabase(at))
        .param("household", household.value())
        .update();
  }

  private InventorySnapshot map(ResultSet row, int rowNumber) throws SQLException {
    OffsetDateTime usedAt = row.getObject("used_at", OffsetDateTime.class);
    UUID actor = row.getObject("actor_user_id", UUID.class);
    return new InventorySnapshot(
        row.getObject("id", UUID.class),
        new HouseholdId(row.getObject("household_id", UUID.class)),
        SnapshotKind.valueOf(row.getString("kind")),
        row.getObject("command_id", UUID.class),
        new UserId(actor == null ? new UUID(0, 0) : actor),
        row.getString("reason"),
        SnapshotPayload.read(row.getString("payload")),
        Timestamps.read(row, "at"),
        usedAt == null ? null : usedAt.toInstant());
  }
}
