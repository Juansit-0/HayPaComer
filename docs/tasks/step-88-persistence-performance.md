# Step 88: Indexes, partitions, and inventory queries

Commit and pull request title: `perf(persistence): indexes, partitions, and inventory queries`

## Goal

Keep the kitchen fast as fridges fill up and sensors report every few seconds for months. Loading and saving a fridge should cost the same number of statements whatever its size, and the fastest-growing table should stay small enough to query and prune by month.

## Scope

- `PostgresFridgeRepository`:
  - loading reads fridges, zones, trays, and items in one ordered join, plus one query for the foods;
  - `findByHousehold` loads every fridge of the household in the same statements instead of one fridge at a time;
  - saving upserts zones, trays, and items in JDBC batches, resolves every food in one query, and deletes what disappeared with one set-based statement per level (`NOT (id = ANY (?))`) instead of row by row.
- Flyway `V20`:
  - `sensor_events` becomes a table partitioned by month on `occurred_at`, with primary key `(id, occurred_at)`, the same indexes, and a DEFAULT partition;
  - existing rows are copied;
  - `ensure_sensor_event_partitions(first_month, months_ahead)` creates missing monthly partitions;
  - ingestion uses `ON CONFLICT (id, occurred_at)`.
- `PostgresSensorPartitions` and the web `SensorPartitionScheduler` keep 3 months ahead, at startup and every night at 03:30.
- Indexes:
  - the existing `(fridge_id, occurred_at)` index serves the cold investigation;
  - `(household_id, type, at)` from V18 serves analytics;
  - no duplicate index was added.

## Measurements

- Before, a fridge with 3 zones and 9 trays took 15 statements to load and more than 100 to save with 54 items.
- Now it takes at most 3 to load and the same fixed number to save with 9 or 54 items (`FridgeQueryCountTest` counts the statements through a counting `DataSource`).
- The migration was run on a development database with real sensor events: rows kept, partitions October 2026 to January 2027 plus DEFAULT.

## Tests (definition of done)

- `FridgeQueryCountTest`: statement counts, tree round trip with order, an empty zone and removals, unknown foods rejected.
- `SensorPartitionsTest`: partitioned table, idempotent maintenance, earlier months created on demand.
- `MigrationsTest` (20 migrations); `SensorIntakeIntegrationTest` and the rest of the suite run on the partitioned table.
