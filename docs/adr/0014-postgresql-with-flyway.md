# ADR 0014: PostgreSQL with Flyway for relational data

- Status: accepted
- Date: 2026-09
- Supersedes: 0002

## Context

Authentication, multiple households, members with roles, food ownership grants, and concurrent writers (web UI, ESP32 events, agent confirmations) exceed what a single-writer SQLite file handles well. The data is highly relational: households, fridges, trays, items, movements, recipes, and plans.

## Decision

Use PostgreSQL 18 for all non-AI data, with schema migrations managed by Flyway and repositories implemented in `adapter-persistence` behind ports defined in `application`. Local development runs PostgreSQL through Docker Compose; integration tests use Testcontainers.

## Consequences

- Real transactions, foreign keys, partial unique indexes, `citext`, `jsonb`, and optimistic locking.
- Schema changes are versioned and reviewed as SQL migrations.
- Requires a running database service; the demo uses Docker Compose.
- The ER model is documented in `docs/database.md`.
