# ADR 0017: haypacomer.dev domain and dev.haypacomer packages

- Status: accepted
- Date: 2026-09

## Context

The brand needs a public domain, and Java convention names packages after the reversed domain. The project started with `com.haypacomer` without owning that domain.

## Decision

Use `haypacomer.dev` as the public domain (web UI at `haypacomer.dev`, API at `api.haypacomer.dev`). Rename the Maven `groupId` and Java packages to `dev.haypacomer`.

## Consequences

- `.dev` is on the HSTS preload list: HTTPS is mandatory everywhere, which matches the security posture.
- One small rename PR before domain code exists keeps the cost minimal.
- CORS allows only `https://haypacomer.dev` in production.
