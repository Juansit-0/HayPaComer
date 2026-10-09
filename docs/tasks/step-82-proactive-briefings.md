# Step 82: Proactive briefings by schedule and events

Commit and pull request title: `feat(agent): proactive briefings by schedule and events`

## Goal

The kitchen speaks first when it matters: a short morning briefing, a nudge when several foods are about to expire, and an explanation of what to check after a fridge alert. Every briefing comes from the same supervisor and tools as the chat, so it only cites measured data, and it never floods the household.

## Scope

- Application:
  - `NotificationType.BRIEFING`;
  - port `HouseholdDirectory` (implemented by `PostgresHouseholdRepository`);
  - port `BriefingLog`, with `RedisBriefingLog` storing `agent:briefing:{household}:{kind}:{date}` for 2 days, so each kind goes out at most once per household and day.
- Agent `proactive`:
  - `Briefer`: runs one specialist as the household owner through the supervisor and publishes a BRIEFING notification (body up to 1500 characters).
  - `ScheduledBriefings`:
    - every run (hourly in production) sends "Today in your kitchen" from the chef at 7:00 in each household's timezone;
    - sends "N foods expire soon" from the coach when 3 or more usable foods are at risk;
    - one failing household never stops the others.
  - `AlertBriefings` (Observer of `NotifyHousehold`):
    - door and cold-chain alerts trigger a cold specialist briefing "What to check after: ...", once per alert type and day;
    - the work runs on a background executor and any failure (for example Redis down) is absorbed there, so alert ingestion never fails because of a briefing;
    - briefings themselves are ignored to avoid loops.
- Web:
  - `briefingDelivery` publishes briefings to the channels and the live stream, and the primary `NotifyHousehold` adds `AlertBriefings` as a third observer;
  - `ProactiveConfiguration` (single daemon executor) and `BriefingScheduler` (`haypacomer.agent.briefings-cron`, default every hour).

## Tests (definition of done)

- `ProactiveBriefingsTest`:
  - the morning briefing goes out once at seven;
  - an expiry cluster brings the coach once;
  - alerts give one background briefing per type and day;
  - a failing household does not stop the others;
  - a broken briefing never breaks the alert.
- `ProactiveBriefingIntegrationTest`: three foods expiring tomorrow put exactly one BRIEFING in the inbox even when the job runs twice.
- `SensorIntakeIntegrationTest` (no Redis) keeps passing: alerts survive without the AI state store.
