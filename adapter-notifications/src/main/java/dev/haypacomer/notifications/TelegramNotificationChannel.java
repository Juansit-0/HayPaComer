package dev.haypacomer.notifications;

import dev.haypacomer.application.notification.ChannelKind;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationPreferences;
import dev.haypacomer.application.port.NotificationChannel;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class TelegramNotificationChannel implements NotificationChannel {

  public static final URI API = URI.create("https://api.telegram.org/");

  private static final Logger LOG = System.getLogger(TelegramNotificationChannel.class.getName());

  private final URI api;
  private final String botToken;
  private final HttpClient http;
  private final Duration timeout;

  public TelegramNotificationChannel(URI api, String botToken, Duration timeout) {
    Objects.requireNonNull(api, "api");
    this.api = api.toString().endsWith("/") ? api : URI.create(api + "/");
    this.botToken = Objects.requireNonNull(botToken, "botToken");
    this.timeout = Objects.requireNonNull(timeout, "timeout");
    if (botToken.isBlank()) {
      throw new IllegalArgumentException("Telegram needs a bot token");
    }
    this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  @Override
  public ChannelKind kind() {
    return ChannelKind.TELEGRAM;
  }

  @Override
  public void deliver(Notification notification, NotificationPreferences recipient) {
    recipient.telegram().ifPresent(chat -> send(chat, notification));
  }

  private void send(String chat, Notification notification) {
    String form =
        "chat_id="
            + URLEncoder.encode(chat, StandardCharsets.UTF_8)
            + "&text="
            + URLEncoder.encode(
                notification.title() + "\n" + notification.body(), StandardCharsets.UTF_8);
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(api + "bot" + botToken + "/sendMessage"))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
    try {
      int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
      if (status >= 300) {
        LOG.log(
            Level.WARNING, "Telegram rejected notification {0}: {1}", notification.id(), status);
      }
    } catch (IOException failure) {
      LOG.log(Level.WARNING, "Telegram is unreachable for notification {0}", notification.id());
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
