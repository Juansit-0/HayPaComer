# ADR 0015: Redis for AI state

- Status: accepted
- Date: 2026-09

## Context

The AI layer produces data with a different shape and lifetime than the domain: agent memory, chat conversations, run traces, audit streams, pending confirmations, response caches, circuit breaker and rate limit state. It is append-only or short-lived, schema-flexible, and must never become the source of truth for stock or safety.

## Decision

Store all AI state in Redis 8 with AOF persistence, using the key map in `docs/database.md`. Ports (`AgentMemoryStore`, `ConversationStore`, `AgentTraceStore`, `PendingConfirmationStore`, `AiResponseCache`) live in `application`; Redis implementations live in `adapter-persistence`. Relational data stays in PostgreSQL.

## Consequences

- TTLs expire conversations, traces, confirmations, and caches without cleanup jobs.
- Streams give ordered traces and a capped audit log.
- Losing Redis loses history and caches, never inventory, grams, or safety decisions.
- One more service in Docker Compose and in health checks.
