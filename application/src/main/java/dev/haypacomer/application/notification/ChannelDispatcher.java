package dev.haypacomer.application.notification;

import dev.haypacomer.application.port.HouseholdRepository;
import dev.haypacomer.application.port.NotificationChannel;
import dev.haypacomer.application.port.NotificationListener;
import dev.haypacomer.application.port.NotificationPreferenceRepository;
import dev.haypacomer.domain.household.Membership;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ChannelDispatcher implements NotificationListener {

  private final HouseholdRepository households;
  private final NotificationPreferenceRepository preferences;
  private final Map<ChannelKind, NotificationChannel> channels = new EnumMap<>(ChannelKind.class);

  public ChannelDispatcher(
      HouseholdRepository households,
      NotificationPreferenceRepository preferences,
      List<NotificationChannel> available) {
    this.households = Objects.requireNonNull(households, "households");
    this.preferences = Objects.requireNonNull(preferences, "preferences");
    available.forEach(channel -> channels.put(channel.kind(), channel));
  }

  @Override
  public void onNotification(Notification notification) {
    households
        .findById(notification.household())
        .ifPresent(
            household ->
                household.memberships().stream()
                    .map(Membership::user)
                    .map(preferences::find)
                    .forEach(
                        recipient ->
                            recipient.channels().stream()
                                .map(channels::get)
                                .filter(Objects::nonNull)
                                .forEach(channel -> channel.deliver(notification, recipient))));
  }
}
