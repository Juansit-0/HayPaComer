# Step 68: Redis stores for agent memory, conversations, traces, and confirmations

Commit and pull request title: `feat(persistence): redis stores for agent memory, conversations, traces, and confirmations`

## Goal

The multi-agent layer (steps 69-72) needs somewhere to keep its state outside PostgreSQL: what the household taught the chef, the chat history, every run with its trace, the writes waiting for a human yes, and an audit of every AI call. All of it lives in Redis, as the key map in `docs/database.md` says.

## Scope

- Application `agent` package: `ConversationId`, `AgentRunId`, `ChatRole`, `ChatMessage` (non-blank, 4000 characters max), `ConversationSummary`, `RunStatus`, `AgentRun` (steps used never exceed the budget), `TraceKind`, `TraceStep`, `PendingConfirmation` (expires after 10 minutes), `AiOutcome`, `AiAuditEntry`.
- Segregated ports: `HouseholdMemory`, `ConversationStore`, `AgentRunStore`, `ConfirmationStore`, `AiAuditLog`.
- Redis adapters in `dev.haypacomer.persistence.redis`:
  - `RedisHouseholdMemory`: hash `agent:memory:{householdId}`, no TTL, key up to 80 and value up to 500 characters.
  - `RedisConversationStore`: stream `agent:conv:{id}` and sorted set `agent:convs:{userId}` by last activity, 7 days.
  - `RedisAgentRunStore`: hash `agent:run:{id}` and stream `agent:trace:{id}`, 30 days.
  - `RedisConfirmationStore`: hash `agent:pending:{id}` that expires with the confirmation, user index `agent:pending:user:{userId}` cleaned on read.
  - `RedisAiAuditLog`: stream `ai:audit` capped near 100000 entries, newest first on read.
- Web `AgentStoresConfiguration` wires the five beans from `StringRedisTemplate`.

## Tests (definition of done)

- `AgentStateTest` (application): budget, expiry, and message validation.
- `RedisAgentStoresTest` (Testcontainers `redis:8-alpine`): memory edits, conversation order and index, run and trace TTL, confirmation TTL and stale index cleanup, audit order.
