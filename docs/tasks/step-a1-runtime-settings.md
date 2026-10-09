# Step A1: Runtime settings in the database with cache

Commit and pull request title: `feat(settings): runtime settings in the database with cache`

Owner: Jenifer Urbano (`Jenifrutica`). First step of plan v2 (version 1.1.0).

## Goal

No rule threshold lives as a constant in the code anymore. Every number that tunes behavior is a row in PostgreSQL with a default, a range, and a description. Each household can override its own values, and reads are fast thanks to an in-memory cache.

## Scope

- Flyway `V21__runtime_settings`:
  - `settings` (key, kind INTEGER or DECIMAL, scope GLOBAL or HOUSEHOLD, value, min, max, description), seeded with 17 keys whose values match the previous constants: door 40 s, 5.0 C, cold chain grace 20 min, scale minimum change 5 g, at risk 2 days, briefing hours 7 and 8, expiry cluster 3, the 2 hour and 30 minute cold rules, AI 20 calls per minute, circuit 3 failures and 60 s, and the session, lockout, and password limits;
  - `household_settings` (household, key, value) for overrides.
- Application:
  - ports `SettingsRepository` (definitions, overrides, override, reset, household of a fridge) and `PolicySource` (freshness, thresholds per household or fridge, briefings, cold rule, circuit, auth, AI calls per minute);
  - `StoredPolicies` reads `PolicySource` from the repository, and household overrides apply only to HOUSEHOLD settings;
  - `FixedPolicies` keeps the old constants for tests and for the old constructors;
  - use cases `ViewHouseholdSettings` (any member), `ChangeHouseholdSetting` (MANAGE_HOUSEHOLD; checks number, whole number, range, and scope; an empty value resets), `ListSettingDefinitions`;
  - `ColdRule` in the domain replaces the 2 hour constants of `ColdInvestigator`, and the verdict text uses the configured limit.
- Consumers read the source instead of the `DEFAULT` constants:
  - `ViewInventory` and `ViewHouseholdMetrics` (at risk days per household);
  - `TrackColdChain` and the fridge monitor registry (thresholds per fridge; a monitor is rebuilt when its thresholds change);
  - `ApplyFridgeScaleReading` (minimum change per household);
  - `InvestigateColdIncidents` (thresholds and cold rule);
  - `ScheduledBriefings` (hours and cluster size per household);
  - `SuggestDishes` and `ReadRecipePhoto` (AI calls per minute);
  - `ProviderCircuit` (circuit policy on every call);
  - `AuthSettings`, read once at startup after Flyway; changes apply after a restart.
- `PostgresSettingsRepository` caches definitions and overrides per household for `haypacomer.settings.cache-ttl` (30 s by default), drops the household entry on every write, and serves the last copy if the database fails.
- REST:
  - `GET /settings/defaults`;
  - `GET /households/{h}/settings`;
  - `PUT /households/{h}/settings` with `{"key": "...", "value": "..."}`, where `null` resets to the default.

## Tests (definition of done)

- `RuntimeSettingsTest`: stored defaults equal the fixed defaults, overrides affect only one household, view, reset, permissions, range, kind, scope, and global keys ignore overrides.
- `ColdRuleTest` and a stricter rule in `ColdInvestigatorTest`.
- `InMemoryFridgeMonitorRegistryTest`: the monitor is rebuilt when the thresholds change.
- `PostgresSettingsRepositoryTest`: seed, upsert and reset, cache until the time to live, and last copy when the tables are gone.
- `SettingsIntegrationTest`: endpoints, 400 out of range, 404 for strangers, 401 anonymous, and the live `PolicySource` sees the change.
