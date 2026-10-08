package dev.haypacomer.persistence.relational;

import dev.haypacomer.application.notification.ChannelKind;
import dev.haypacomer.application.notification.NotificationPreferences;
import dev.haypacomer.application.port.NotificationPreferenceRepository;
import dev.haypacomer.domain.identity.UserId;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresNotificationPreferenceRepository implements NotificationPreferenceRepository {

  private final JdbcClient jdbc;

  public PostgresNotificationPreferenceRepository(DataSource dataSource) {
    this.jdbc = JdbcClient.create(dataSource);
  }

  @Override
  public NotificationPreferences find(UserId user) {
    return jdbc.sql(
            "SELECT channels, telegram_chat_id FROM notification_preferences WHERE user_id = :user")
        .param("user", user.value())
        .query(
            (row, rowNumber) ->
                new NotificationPreferences(
                    user, channels(row.getArray("channels")), row.getString("telegram_chat_id")))
        .optional()
        .orElseGet(() -> NotificationPreferences.defaults(user));
  }

  @Override
  public void save(NotificationPreferences preferences) {
    jdbc.sql(
            """
            INSERT INTO notification_preferences (user_id, channels, telegram_chat_id)
            VALUES (:user, string_to_array(:channels, ','), :chat)
            ON CONFLICT (user_id) DO UPDATE SET channels = EXCLUDED.channels,
                telegram_chat_id = EXCLUDED.telegram_chat_id
            """)
        .param("user", preferences.user().value())
        .param(
            "channels",
            preferences.channels().stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(",")))
        .param("chat", preferences.telegram().orElse(null), Types.VARCHAR)
        .update();
  }

  private static Set<ChannelKind> channels(Array array) throws SQLException {
    return Arrays.stream((String[]) array.getArray())
        .filter(name -> !name.isBlank())
        .map(ChannelKind::valueOf)
        .collect(Collectors.toSet());
  }
}
