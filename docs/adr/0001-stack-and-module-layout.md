# ADR 0001: Stack and module layout

- Status: accepted
- Date: 2026-09

## Context

The project needs a professional Java stack for a course project, with a domain core testable without infrastructure and clear boundaries that scale over ~79 incremental PRs.

## Decision

Use Java 25 LTS, Spring Boot 4.1.1, and a Maven multi-module build with 8 single-responsibility modules: `domain`, `application`, `adapter-persistence`, `adapter-sensors`, `adapter-ai`, `adapter-notifications`, `agent`, and `web`.

## Consequences

- `domain` and `application` stay framework-free and fully unit-testable.
- Each module can evolve and be tested in isolation.
- The build is slightly more verbose and requires boundary enforcement tooling.
