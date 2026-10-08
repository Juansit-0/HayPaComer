package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.notification.ChannelKind;
import dev.haypacomer.application.notification.InboxEntry;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationPreferences;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.application.port.NotificationChannel;
import dev.haypacomer.application.port.NotificationInbox;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresNotificationInbox implements NotificationInbox, NotificationChannel {

  private final JdbcClient jdbc;

  public PostgresNotificationInbox(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public ChannelKind kind() {
    return ChannelKind.WEB;
  }

  @Override
  public void deliver(Notification notification, NotificationPreferences recipient) {
    jdbc.sql(
            """
            INSERT INTO notifications (id, user_id, household_id, type, title, body, created_at)
            VALUES (:id, :user, :household, :type, :title, :body, :at)
            ON CONFLICT (id, user_id) DO NOTHING
            """)
        .param("id", notification.id())
        .param("user", recipient.user().value())
        .param("household", notification.household().value())
        .param("type", notification.type().name())
        .param("title", notification.title())
        .param("body", notification.body())
        .param("at", Timestamps.toDatabase(notification.at()))
        .update();
  }

  @Override
  public List<InboxEntry> recent(UserId user, int limit) {
    return jdbc.sql(
            """
            SELECT id, household_id, type, title, body, created_at, read_at FROM notifications
            WHERE user_id = :user ORDER BY created_at DESC, id LIMIT :limit
            """)
        .param("user", user.value())
        .param("limit", limit)
        .query(
            (row, rowNumber) ->
                new InboxEntry(
                    new Notification(
                        row.getObject("id", UUID.class),
                        new HouseholdId(row.getObject("household_id", UUID.class)),
                        NotificationType.valueOf(row.getString("type")),
                        row.getString("title"),
                        row.getString("body"),
                        Timestamps.read(row, "created_at")),
                    Optional.ofNullable(row.getObject("read_at", OffsetDateTime.class))
                        .map(OffsetDateTime::toInstant)
                        .orElse(null)))
        .list();
  }

  @Override
  public boolean markRead(UserId user, UUID notification, Instant at) {
    return jdbc.sql(
                """
                UPDATE notifications SET read_at = COALESCE(read_at, :at)
                WHERE id = :id AND user_id = :user
                """)
            .param("at", Timestamps.toDatabase(at))
            .param("id", notification)
            .param("user", user.value())
            .update()
        == 1;
  }
}
