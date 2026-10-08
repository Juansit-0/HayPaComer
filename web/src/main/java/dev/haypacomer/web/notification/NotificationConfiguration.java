package dev.haypacomer.web.notification;

import dev.haypacomer.application.live.AlertsToLive;
import dev.haypacomer.application.live.BroadcastLiveUpdate;
import dev.haypacomer.application.notification.ChannelDispatcher;
import dev.haypacomer.application.notification.ListNotifications;
import dev.haypacomer.application.notification.MarkNotificationRead;
import dev.haypacomer.application.notification.NotifyHousehold;
import dev.haypacomer.application.notification.UpdateNotificationPreferences;
import dev.haypacomer.application.notification.ViewNotificationPreferences;
import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.NotificationChannel;
import dev.haypacomer.application.port.NotificationPreferenceRepository;
import dev.haypacomer.notifications.LogNotificationChannel;
import dev.haypacomer.notifications.TelegramNotificationChannel;
import dev.haypacomer.persistence.relational.PostgresNotificationInbox;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfiguration {

  @Bean
  NotifyHousehold notifyHousehold(
      HouseholdRepository households,
      NotificationPreferenceRepository preferences,
      PostgresNotificationInbox inbox,
      BroadcastLiveUpdate live,
      @Value("${haypacomer.notifications.telegram.bot-token:}") String telegramToken) {
    List<NotificationChannel> channels = new ArrayList<>();
    channels.add(inbox);
    channels.add(new LogNotificationChannel());
    if (!telegramToken.isBlank()) {
      channels.add(
          new TelegramNotificationChannel(
              TelegramNotificationChannel.API, telegramToken, Duration.ofSeconds(5)));
    }
    return new NotifyHousehold(
        List.of(new ChannelDispatcher(households, preferences, channels), new AlertsToLive(live)));
  }

  @Bean
  ListNotifications listNotifications(PostgresNotificationInbox inbox) {
    return new ListNotifications(inbox);
  }

  @Bean
  MarkNotificationRead markNotificationRead(PostgresNotificationInbox inbox, Clock clock) {
    return new MarkNotificationRead(inbox, clock);
  }

  @Bean
  ViewNotificationPreferences viewNotificationPreferences(
      NotificationPreferenceRepository preferences) {
    return new ViewNotificationPreferences(preferences);
  }

  @Bean
  UpdateNotificationPreferences updateNotificationPreferences(
      NotificationPreferenceRepository preferences) {
    return new UpdateNotificationPreferences(preferences);
  }
}
