# ADR 0002: SQLite persistence over JDBC

- Status: accepted
- Date: 2026-09

## Context

A single household runs one fridge with local data. The system must work offline, be easy to demo, and avoid heavy infrastructure.

## Decision

Use SQLite with WAL, indexes, and prepared statements through plain JDBC, behind repository ports defined in `application`.

## Consequences

- Zero-install persistence, easy backups, and transparent SQL.
- No ORM magic; schema and queries are explicit and reviewed.
- Concurrency is limited to one writer, which matches the single-household scope.
