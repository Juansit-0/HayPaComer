# Step 80: Degraded mode with cache and retries (Proxy)

Commit and pull request title: `feat(application): degraded mode with cache and retries (proxy)`

## Goal

Short database hiccups should not take the kitchen down, and a longer outage should be visible instead of silent. Reference data (food catalog, substitution rules, recipe templates, prices) can safely come from the last saved copy; measured stock and grams never can, so those requests fail honestly.

## Scope

- Application:
  - port `ServiceHealth` with `DegradedComponent`;
  - `ViewServiceStatus` returns a `ServiceStatus` (healthy or the degraded components by name).
- `adapter-persistence` `resilience` (framework-free):
  - `RetryPolicy`: attempts with growing pauses; business errors (`IllegalArgumentException`, `IllegalStateException`) are never retried; interrupts are respected.
  - `ResilientReads`: retry, then the last saved copy per key (bounded at 1000 entries), and `ServiceHealth` marks the component degraded since the first failure and recovered on the next success; writes retry and forget saved copies.
  - Proxies with the same port interfaces: `ResilientFoodCatalog`, `ResilientSubstitutionRules`, `ResilientRecipeTemplates`, `ResilientFoodPrices`.
  - `InMemoryServiceHealth`.
- Web:
  - `ResilienceConfiguration` makes the proxies the `@Primary` beans around the Postgres repositories (3 attempts, 100 ms then 200 ms).
  - `GET /api/v1/status` answers `OK` or `DEGRADED` with components, reasons, and since when.
  - An unreachable database (`DataAccessResourceFailureException`, `TransientDataAccessException`) answers 503 "Service unavailable" with `Retry-After: 5`.
  - The UI shows a "Running in saved mode" notice when the status is degraded.

## Tests (definition of done)

- `ResilientReadsTest`: retries with pauses, last saved copy and degraded reporting until recovery, writes clear copies, business errors pass through, every proxy, retry validation and interrupts.
- `ViewServiceStatusTest`, `DataUnavailableTest` (503 with `Retry-After`), `AnalyticsIntegrationTest` (proxies are the injected beans, status OK).
