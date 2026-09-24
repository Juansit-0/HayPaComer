# ADR 0010: Secrets only in environment variables

- Status: accepted
- Date: 2026-09

## Context

The system integrates AI providers and notification channels that require API keys and tokens. Leaked keys in a public repository are a real risk.

## Decision

Store secrets only in environment variables loaded from a local `.env` file. The `.gitignore` excludes `.env` and variants. The repository never contains credentials, and documentation shows placeholders only.

## Consequences

- Safe public repository and clean history.
- Each environment needs its own `.env`; the demo requires a documented setup step.
- Tests use fakes instead of real credentials.
