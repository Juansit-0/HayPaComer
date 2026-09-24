# ADR 0006: 23/23 GoF pattern coverage with tests and justification

- Status: accepted
- Date: 2026-09

## Context

The project must demonstrate pattern mastery honestly, not with decorative or forced usages.

## Decision

Cover all 23 GoF patterns, each in a real production flow, with a dedicated test and a justified row in `docs/patterns.md`.

## Consequences

- Strong design vocabulary across modules and a reviewable mapping.
- Risk of over-engineering when a pattern is unnecessary; the honesty rule requires a real flow.
- Each pattern PR must include its test and documentation update.
