package dev.haypacomer.application.notification;

import dev.haypacomer.domain.identity.UserId;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record NotificationPreferences(
    UserId user, Set<ChannelKind> channels, String telegramChatId) {

  public NotificationPreferences {
    Objects.requireNonNull(user, "user");
    channels = channels.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(channels));
    if (telegramChatId != null) {
      telegramChatId = telegramChatId.strip();
      if (!telegramChatId.matches("-?\\d{1,20}")) {
        throw new IllegalArgumentException("A Telegram chat id is a number");
      }
    }
    if (channels.contains(ChannelKind.TELEGRAM) && telegramChatId == null) {
      throw new IllegalArgumentException("Telegram needs a chat id");
    }
  }

  public static NotificationPreferences defaults(UserId user) {
    return new NotificationPreferences(user, Set.of(ChannelKind.WEB, ChannelKind.LOG), null);
  }

  public Optional<String> telegram() {
    return Optional.ofNullable(telegramChatId);
  }
}
