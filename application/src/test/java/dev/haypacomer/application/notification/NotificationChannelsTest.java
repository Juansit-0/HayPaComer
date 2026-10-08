package dev.haypacomer.application.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.port.NotificationChannel;
import dev.haypacomer.application.port.NotificationInbox;
import dev.haypacomer.application.port.NotificationPreferenceRepository;
import dev.haypacomer.application.support.InMemoryHouseholdRepository;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.household.HouseholdId;
import dev.haypacomer.domain.household.Role;
import dev.haypacomer.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationChannelsTest {

  private static final Instant NOW = Instant.parse("2026-10-08T21:00:00Z");

  private final InMemoryHouseholdRepository households = new InMemoryHouseholdRepository();
  private final Map<UserId, NotificationPreferences> stored = new HashMap<>();
  private final NotificationPreferenceRepository preferences =
      new NotificationPreferenceRepository() {
        @Override
        public NotificationPreferences find(UserId user) {
          return stored.getOrDefault(user, NotificationPreferences.defaults(user));
        }

        @Override
        public void save(NotificationPreferences value) {
          stored.put(value.user(), value);
        }
      };
  private final List<String> deliveries = new ArrayList<>();
  private final UserId juan = UserId.newId();
  private final UserId ana = UserId.newId();
  private Household household;

  private NotificationChannel channel(ChannelKind kind) {
    return new NotificationChannel() {
      @Override
      public ChannelKind kind() {
        return kind;
      }

      @Override
      public void deliver(Notification notification, NotificationPreferences recipient) {
        deliveries.add(
            kind
                + ":"
                + (recipient.user().equals(juan) ? "juan" : "ana")
                + recipient.telegram().map(chat -> "@" + chat).orElse(""));
      }
    };
  }

  @BeforeEach
  void setUp() {
    household =
        Household.create("Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan, NOW);
    household.join(ana, Role.MEMBER, NOW);
    households.save(household);
  }

  private Notification doorOpen(HouseholdId target) {
    return Notification.of(target, NotificationType.DOOR_LEFT_OPEN, "Door", "Open", NOW);
  }

  @Test
  void everyObserverHearsEveryNotification() {
    List<Notification> first = new ArrayList<>();
    List<Notification> second = new ArrayList<>();
    NotifyHousehold subject = new NotifyHousehold(List.of(first::add, second::add));

    subject.publish(doorOpen(household.id()));
    NotifyHousehold.NOBODY.publish(doorOpen(household.id()));

    assertEquals(1, first.size());
    assertEquals(first, second);
  }

  @Test
  void eachMemberGetsTheChannelsTheyChose() {
    new UpdateNotificationPreferences(preferences)
        .update(ana, Set.of(ChannelKind.TELEGRAM, ChannelKind.WEB), "123456");
    ChannelDispatcher dispatcher =
        new ChannelDispatcher(
            households,
            preferences,
            List.of(channel(ChannelKind.WEB), channel(ChannelKind.TELEGRAM)));

    dispatcher.onNotification(doorOpen(household.id()));
    dispatcher.onNotification(doorOpen(HouseholdId.newId()));

    assertEquals(3, deliveries.size());
    assertTrue(deliveries.contains("WEB:juan"));
    assertTrue(deliveries.contains("WEB:ana@123456"));
    assertTrue(deliveries.contains("TELEGRAM:ana@123456"));
    assertEquals(
        Set.of(ChannelKind.WEB, ChannelKind.LOG),
        new ViewNotificationPreferences(preferences).view(juan).channels());
  }

  @Test
  void preferencesAreValidated() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new NotificationPreferences(juan, Set.of(ChannelKind.TELEGRAM), null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new NotificationPreferences(juan, Set.of(ChannelKind.TELEGRAM), "@juan"));
    assertTrue(new NotificationPreferences(juan, Set.of(), null).channels().isEmpty());
    assertEquals(
        "-1001",
        new NotificationPreferences(juan, Set.of(ChannelKind.TELEGRAM), " -1001 ")
            .telegram()
            .orElseThrow());
  }

  @Test
  void theWebInboxListsAndMarksOnlyTheCallersNotifications() {
    Notification door = doorOpen(household.id());
    Map<UUID, Instant> read = new HashMap<>();
    NotificationInbox inbox =
        new NotificationInbox() {
          @Override
          public List<InboxEntry> recent(UserId user, int limit) {
            assertEquals(ListNotifications.LIMIT, limit);
            return user.equals(juan)
                ? List.of(new InboxEntry(door, read.get(door.id())))
                : List.of();
          }

          @Override
          public boolean markRead(UserId user, UUID notification, Instant at) {
            if (!user.equals(juan) || !notification.equals(door.id())) {
              return false;
            }
            read.put(notification, at);
            return true;
          }
        };
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    assertFalse(new ListNotifications(inbox).list(juan).getFirst().read().isPresent());
    new MarkNotificationRead(inbox, clock).mark(juan, door.id());
    assertEquals(NOW, new ListNotifications(inbox).list(juan).getFirst().read().orElseThrow());
    assertTrue(new ListNotifications(inbox).list(ana).isEmpty());
    assertThrows(
        NotificationNotFoundException.class,
        () -> new MarkNotificationRead(inbox, clock).mark(ana, door.id()));
  }
}
