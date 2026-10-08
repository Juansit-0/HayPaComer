package dev.haypacomer.persistence.relational;

import static dev.haypacomer.persistence.relational.PersistenceFixtures.NOW;
import static dev.haypacomer.persistence.relational.PersistenceFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.haypacomer.application.notification.ChannelKind;
import dev.haypacomer.application.notification.InboxEntry;
import dev.haypacomer.application.notification.Notification;
import dev.haypacomer.application.notification.NotificationPreferences;
import dev.haypacomer.application.notification.NotificationType;
import dev.haypacomer.domain.household.Household;
import dev.haypacomer.domain.identity.User;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostgresNotificationsTest extends PostgresTestSupport {

  @Test
  void storesTheWebInboxPerUserAndMarksReadOnce() {
    User juan = user("juan@haypacomer.dev", "Juan");
    User ana = user("ana@haypacomer.dev", "Ana");
    PostgresUserRepository users = new PostgresUserRepository(dataSource);
    users.save(juan);
    users.save(ana);
    Household household =
        Household.create(
            "Apartment", Currency.getInstance("COP"), ZoneId.of("UTC"), juan.id(), NOW);
    new PostgresHouseholdRepository(dataSource).save(household);
    PostgresNotificationInbox inbox = new PostgresNotificationInbox(dataSource);
    Notification door =
        Notification.of(household.id(), NotificationType.DOOR_LEFT_OPEN, "Door", "Open", NOW);
    Notification warm =
        Notification.of(
            household.id(), NotificationType.COLD_CHAIN_BREACH, "Warm", "6 C", NOW.plusSeconds(60));

    inbox.deliver(door, NotificationPreferences.defaults(juan.id()));
    inbox.deliver(door, NotificationPreferences.defaults(juan.id()));
    inbox.deliver(warm, NotificationPreferences.defaults(juan.id()));
    inbox.deliver(door, NotificationPreferences.defaults(ana.id()));

    List<InboxEntry> forJuan = inbox.recent(juan.id(), 50);
    assertEquals(List.of(warm, door), forJuan.stream().map(InboxEntry::notification).toList());
    assertTrue(forJuan.getFirst().read().isEmpty());
    assertTrue(inbox.markRead(juan.id(), door.id(), NOW.plusSeconds(120)));
    assertTrue(inbox.markRead(juan.id(), door.id(), NOW.plusSeconds(300)));
    assertEquals(NOW.plusSeconds(120), inbox.recent(juan.id(), 50).get(1).read().orElseThrow());
    assertTrue(inbox.recent(ana.id(), 50).getFirst().read().isEmpty());
    assertFalse(inbox.markRead(ana.id(), warm.id(), NOW));
    assertFalse(inbox.markRead(juan.id(), UUID.randomUUID(), NOW));
    assertEquals(1, inbox.recent(juan.id(), 1).size());
    assertEquals(ChannelKind.WEB, inbox.kind());
  }

  @Test
  void keepsChannelPreferencesWithDefaults() {
    User juan = user("juan@haypacomer.dev", "Juan");
    new PostgresUserRepository(dataSource).save(juan);
    PostgresNotificationPreferenceRepository preferences =
        new PostgresNotificationPreferenceRepository(dataSource);

    assertEquals(NotificationPreferences.defaults(juan.id()), preferences.find(juan.id()));
    NotificationPreferences telegram =
        new NotificationPreferences(
            juan.id(), Set.of(ChannelKind.TELEGRAM, ChannelKind.WEB), "123456");
    preferences.save(telegram);
    assertEquals(telegram, preferences.find(juan.id()));
    NotificationPreferences silent = new NotificationPreferences(juan.id(), Set.of(), null);
    preferences.save(silent);
    assertEquals(silent, preferences.find(juan.id()));
  }
}
