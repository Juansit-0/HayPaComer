# ADR 0009: Local git during foundation and branding

- Status: accepted
- Date: 2026-09

## Context

The public repository cannot exist yet (ADR 0008), but work must be versioned from day one with small steps.

## Decision

During foundation and branding, commit locally on `main` with Conventional Commits in English. After repository creation, every step goes through a `feat/*` branch, a PR, green CI, review approval, and a squash merge, protected by branch protection.

## Consequences

- Full history from the first day.
- The early history has no PRs, which is documented and intentional.
- After the repository exists, `main` accepts changes only through approved PRs.
