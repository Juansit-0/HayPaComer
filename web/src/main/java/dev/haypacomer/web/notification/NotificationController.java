package dev.haypacomer.web.notification;

import dev.haypacomer.application.notification.ChannelKind;
import dev.haypacomer.application.notification.InboxEntry;
import dev.haypacomer.application.notification.ListNotifications;
import dev.haypacomer.application.notification.MarkNotificationRead;
import dev.haypacomer.application.notification.NotificationPreferences;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.application.notification.UpdateNotificationPreferences;
import dev.haypacomer.application.notification.ViewNotificationPreferences;
import dev.haypacomer.web.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

  private final ListNotifications listNotifications;
  private final MarkNotificationRead markNotificationRead;
  private final ViewNotificationPreferences viewPreferences;
  private final UpdateNotificationPreferences updatePreferences;

  public NotificationController(
      ListNotifications listNotifications,
      MarkNotificationRead markNotificationRead,
      ViewNotificationPreferences viewPreferences,
      UpdateNotificationPreferences updatePreferences) {
    this.listNotifications = listNotifications;
    this.markNotificationRead = markNotificationRead;
    this.viewPreferences = viewPreferences;
    this.updatePreferences = updatePreferences;
  }

  @GetMapping("/api/v1/notifications")
  List<NotificationResponse> notifications(@AuthenticationPrincipal Jwt jwt) {
    return listNotifications.list(CurrentUser.of(jwt)).stream()
        .map(NotificationResponse::from)
        .toList();
  }

  @PatchMapping("/api/v1/notifications/{notificationId}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID notificationId) {
    markNotificationRead.mark(CurrentUser.of(jwt), notificationId);
  }

  @GetMapping("/api/v1/me/notification-preferences")
  PreferencesResponse preferences(@AuthenticationPrincipal Jwt jwt) {
    return PreferencesResponse.from(viewPreferences.view(CurrentUser.of(jwt)));
  }

  @PutMapping("/api/v1/me/notification-preferences")
  PreferencesResponse updatePreferences(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PreferencesRequest request) {
    return PreferencesResponse.from(
        updatePreferences.update(
            CurrentUser.of(jwt), request.channels(), request.telegramChatId()));
  }

  record PreferencesRequest(@NotNull Set<ChannelKind> channels, String telegramChatId) {}

  record PreferencesResponse(Set<ChannelKind> channels, String telegramChatId) {

    static PreferencesResponse from(NotificationPreferences preferences) {
      return new PreferencesResponse(preferences.channels(), preferences.telegram().orElse(null));
    }
  }

  record NotificationResponse(
      UUID id,
      UUID householdId,
      NotificationType type,
      String title,
      String body,
      Instant at,
      Instant readAt) {

    static NotificationResponse from(InboxEntry entry) {
      return new NotificationResponse(
          entry.notification().id(),
          entry.notification().household().value(),
          entry.notification().type(),
          entry.notification().title(),
          entry.notification().body(),
          entry.notification().at(),
          entry.read().orElse(null));
    }
  }
}
