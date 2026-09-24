# ADR 0007: Offline-first with degraded mode and Circuit Breaker

- Status: accepted
- Date: 2026-09

## Context

Home networks fail, AI providers go down, and the fridge must keep working. Losing recommendations because a remote API is unreachable is unacceptable.

## Decision

Design offline-first: local rules, caches, and a Circuit Breaker in front of AI calls with a rule-based fallback planner. Inventory, alerts, scale flows, and the market list never depend on connectivity.

## Consequences

- The demo includes an AI outage where the system keeps working.
- Additional code for caches, timeouts, and fallback paths that must be tested.
- Slightly stale data is accepted over unavailable features.
