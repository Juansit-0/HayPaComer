package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.audit.AuditEntry;
import dev.haypacomer.application.port.AuditLog;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresAuditLog implements AuditLog {

  private final JdbcClient jdbc;

  public PostgresAuditLog(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public void record(AuditEntry entry) {
    long id =
        jdbc.sql(
                """
                INSERT INTO audit_log (actor_user_id, household_id, action, entity, entity_id,
                                       detail, at)
                VALUES (:actor, :household, :action, :entity, :entityId, '{}'::jsonb, :at)
                RETURNING id
                """)
            .param("actor", entry.actor().value())
            .param("household", entry.household().value())
            .param("action", entry.action())
            .param("entity", entry.entity())
            .param("entityId", entry.entityId())
            .param("at", Timestamps.toDatabase(entry.at()))
            .query(Long.class)
            .single();
    entry
        .detail()
        .forEach(
            (key, value) ->
                jdbc.sql(
                        "UPDATE audit_log SET detail = detail || jsonb_build_object(:key::text,"
                            + " :value::text) WHERE id = :id")
                    .param("key", key)
                    .param("value", value)
                    .param("id", id)
                    .update());
  }

  @Override
  public List<AuditEntry> recent(HouseholdId household, int limit) {
    return jdbc
        .sql(
            """
            SELECT id, actor_user_id, action, entity, entity_id, at FROM audit_log
            WHERE household_id = :household ORDER BY at DESC, id DESC LIMIT :limit
            """)
        .param("household", household.value())
        .param("limit", limit)
        .query(
            (row, rowNumber) ->
                new Row(
                    row.getLong("id"),
                    row.getObject("actor_user_id", UUID.class),
                    row.getString("action"),
                    row.getString("entity"),
                    row.getObject("entity_id", UUID.class),
                    Timestamps.read(row, "at")))
        .list()
        .stream()
        .map(row -> row.toEntry(household, detail(row.id())))
        .toList();
  }

  private Map<String, String> detail(long id) {
    Map<String, String> detail = new LinkedHashMap<>();
    jdbc.sql(
            "SELECT key, value FROM audit_log, jsonb_each_text(detail) WHERE id = :id ORDER BY key")
        .param("id", id)
        .query(
            (row, rowNumber) -> {
              detail.put(row.getString("key"), row.getString("value"));
              return row.getString("key");
            })
        .list();
    return detail;
  }

  private record Row(long id, UUID actor, String action, String entity, UUID entityId, Instant at) {

    AuditEntry toEntry(HouseholdId household, Map<String, String> detail) {
      return new AuditEntry(
          new UserId(actor == null ? new UUID(0, 0) : actor),
          household,
          action,
          entity,
          entityId,
          detail,
          at);
    }
  }
}
