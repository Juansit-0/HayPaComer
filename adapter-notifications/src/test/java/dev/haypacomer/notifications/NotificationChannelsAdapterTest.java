package dev.haypacomer.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import dev.haypacomer.application.notification.ChannelKind;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationPreferences;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.identity.UserId;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationChannelsAdapterTest {

  private static final Notification DOOR =
      Notification.of(
          HouseholdId.newId(),
          NotificationType.DOOR_LEFT_OPEN,
          "Fridge door left open",
          "Close it",
          Instant.parse("2026-10-08T21:00:00Z"));

  private HttpServer server;
  private final AtomicReference<String> path = new AtomicReference<>();
  private final AtomicReference<String> body = new AtomicReference<>();
  private volatile int status = 200;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          path.set(exchange.getRequestURI().getPath());
          body.set(
              URLDecoder.decode(
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                  StandardCharsets.UTF_8));
          exchange.sendResponseHeaders(status, -1);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private TelegramNotificationChannel telegram() {
    return new TelegramNotificationChannel(
        URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"),
        "123:token",
        Duration.ofSeconds(2));
  }

  @Test
  void telegramSendsTheTitleAndBodyToTheChat() {
    telegram()
        .deliver(
            DOOR,
            new NotificationPreferences(UserId.newId(), Set.of(ChannelKind.TELEGRAM), "987654"));

    assertEquals("/bot123:token/sendMessage", path.get());
    assertTrue(body.get().contains("chat_id=987654"));
    assertTrue(body.get().contains("text=Fridge door left open\nClose it"));
    assertEquals(ChannelKind.TELEGRAM, telegram().kind());
  }

  @Test
  void telegramNeverBreaksTheAlertFlow() {
    telegram().deliver(DOOR, NotificationPreferences.defaults(UserId.newId()));
    assertNull(path.get());

    status = 500;
    NotificationPreferences withChat =
        new NotificationPreferences(UserId.newId(), Set.of(ChannelKind.TELEGRAM), "1");
    telegram().deliver(DOOR, withChat);
    server.stop(0);
    telegram().deliver(DOOR, withChat);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TelegramNotificationChannel(
                TelegramNotificationChannel.API, " ", Duration.ofSeconds(1)));
  }

  @Test
  void logChannelWritesOnlyTheTitle() {
    LogNotificationChannel log = new LogNotificationChannel();

    log.deliver(DOOR, NotificationPreferences.defaults(UserId.newId()));

    assertEquals(ChannelKind.LOG, log.kind());
    assertFalse(DOOR.title().isBlank());
  }
}
