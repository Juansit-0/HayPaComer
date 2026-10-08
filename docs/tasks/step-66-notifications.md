# Step 66: Telegram, web, and log notification channels (observer, strategy)

Commit and pull request title: `feat(notifications): telegram, web, and log channels (observer, strategy)`

## Goal

When the fridge detects a problem (door left open, fridge too warm), every member of the household hears about it on the channels they chose: the web inbox, Telegram, or the server log.

## Patterns

- Observer: `NotifyHousehold` is the subject. Fridge alerts (`AlertDispatcher`, used by `ObserveSensorEvent` and `CheckFridgeAlerts`) publish a `Notification`; every `NotificationListener` registered in the subject receives it.
- Strategy: `ChannelDispatcher` (a listener) picks, per member, the `NotificationChannel` implementations that match their `NotificationPreferences` (`WEB`, `TELEGRAM`, `LOG`). Each channel is one strategy: `PostgresNotificationInbox`, `TelegramNotificationChannel`, `LogNotificationChannel`.

## Scope

- Application `notification`: `Notification`, `NotificationType`, `ChannelKind`, `NotificationPreferences` (default WEB and LOG; Telegram needs a numeric chat id), `NotifyHousehold`, `ChannelDispatcher`, `ListNotifications`, `MarkNotificationRead`, `ViewNotificationPreferences`, `UpdateNotificationPreferences`; ports `NotificationListener`, `NotificationChannel`, `NotificationInbox`, `NotificationPreferenceRepository`.
- Adapters: Telegram over the JDK `HttpClient` (never breaks the alert flow on errors), log channel, Postgres inbox and preferences (Flyway `V17`).
- Web: `GET /notifications`, `PATCH /notifications/{id}/read`, `GET` and `PUT /me/notification-preferences`; Telegram is active only when `TELEGRAM_BOT_TOKEN` is set.

## Tests (definition of done)

- Every observer receives each notification; members get only the channels they chose.
- A door left open and a warm fridge publish one notification each; stock changes do not.
- Inbox per user, mark read once, preferences round trip, Telegram request shape and failure tolerance.
