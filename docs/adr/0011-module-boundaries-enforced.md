# ADR 0011: Module boundaries enforced by enforcer and ArchUnit

- Status: accepted
- Date: 2026-09

## Context

A clean module layout (ADR 0001) degrades silently when a framework import leaks into the core or an adapter reaches into another one.

## Decision

Enforce boundaries in the build: `maven-enforcer` bans framework dependencies in `domain` and `application`, and ArchUnit tests verify package dependency rules, including that nobody depends on `web`.

## Consequences

- Boundary violations fail the build instead of being caught in review.
- Slight build-time cost and maintenance of rule sets as modules grow.
- The architecture stays executable and documented.
