# Architecture

## Context

HayPaComer is a smart home fridge: a Java backend, an ESP32 module (door, temperature, scale), a kitchen scale, and a web UI. The ESP32 measures; Java interprets events and decides. All business logic, AI, and the agent run in the backend; clients use the REST API (`docs/api.md`) and SSE.

## Modules

| Module | Responsibility | May depend on |
|---|---|---|
| domain | Model and business rules (pure Java) | nothing |
| application | Use cases and ports | domain |
| adapter-persistence | PostgreSQL repositories (Flyway) and Redis stores for AI state | application, domain |
| adapter-sensors | ESP32 events and simulator | application, domain |
| adapter-ai | Offline rules, Gemini / OpenAI-compatible | application, domain |
| adapter-notifications | Telegram, web, log channels | application, domain |
| agent | Runtime, tools, memory, supervisor | application, domain |
| web | Spring: REST, SSE, UI, security (JWT), OpenAPI, Actuator | all modules |

Rules:

- No frameworks in `domain`, `application`, and `agent` (enforced by maven-enforcer).
- Adapters are isolated from each other; nobody depends on `web` (ArchUnit).
- Framework annotations only in `web` and adapters.
- One use case per class with a single public method; ports segregated by interface.

## Data flow

```text
ESP32 (door, temperature, weight) -> X-Device-Key filter -> REST event intake (web)
  -> validation chain (adapter-sensors)
  -> use cases (application) -> domain rules
  -> persistence (adapter-persistence, PostgreSQL)
  -> events (Observer) -> SSE panel, notifications, analytics

Web UI -> JWT filter -> household and ownership authorization -> REST controllers (web)
  -> use cases (application) -> domain rules -> PostgreSQL
```

Agent flow:

```text
Trigger -> AgentRuntime (plan, tool call, observe) -> GuardrailChain
  -> ToolRegistry (real Java services) -> response with evidence and visible trace
  memory, conversations, traces, pending confirmations, AI cache -> Redis
  approved confirmation -> real use case -> PostgreSQL
```

## Adapter pattern

The Adapter pattern lives in the project code, not only in the framework, at two levels.

Ports and adapters (hexagonal): `application` defines ports; `adapter-*` modules implement them.

| Port (application) | Adapter (module) |
|---|---|
| `InventoryRepository`, `HouseholdRepository`, `UserRepository`, ... | `PostgresInventoryRepository`, ... (adapter-persistence) |
| `AgentMemoryStore`, `ConversationStore`, `AgentTraceStore`, `PendingConfirmationStore`, `AiResponseCache` | `RedisAgentMemoryStore`, ... (adapter-persistence) |
| `RecommendationEngine` | `RuleBasedRecommendationEngine`, `GeminiRecommendationAdapter`, `OpenAiCompatibleRecommendationAdapter` (adapter-ai) |
| `NotificationChannel` | `TelegramChannel`, `WebChannel`, `LogChannel` (adapter-notifications) |
| `SensorEventDecoder` | `Esp32EventAdapter` with `Esp32Simulator` for hardware-free demos (adapter-sensors) |
| `PasswordHasher`, `TokenIssuer` | `BCryptPasswordHasher`, `JwtTokenIssuer` (web) |

GoF Adapter (class level), each with its own test and row in `patterns.md`:

- `Esp32EventAdapter`: ESP32 JSON envelope or batch -> domain `SensorEvent`s, using one Factory Method creator per event type.
- `Hx711ReadingAdapter`: raw HX711 counts plus calibration -> `Grams`.
- `GeminiRecommendationAdapter`, `OpenAiCompatibleRecommendationAdapter`: provider APIs -> `RecommendationEngine`.
- `OcrLabelReaderAdapter`, `QrLabelAdapter`: external libraries -> `LabelReader`.

## Security

- Access JWT (15 min) plus rotating refresh token stored hashed in PostgreSQL; BCrypt passwords.
- Household roles `OWNER`, `MEMBER`, `GUEST`; food ownership checked in `domain`.
- ESP32 devices use a revocable per-device API key.
- Details in ADR 0016 and `docs/database.md`.

## Key mechanisms

- Idempotent event ingestion with client-generated event ids.
- Measurements are accepted only when stable; tare before weighing.
- Cold-chain risk is decided by explicit rules, never by AI.
- Offline-first: degraded mode with caches and Circuit Breaker.
- RFC 7807 error responses and a global exception handler.
- PostgreSQL with Flyway migrations, constraints, and indexes; Redis with TTLs for AI state.

## Quality gates

- JUnit 5 unit and integration tests; MockMvc for web; Testcontainers (PostgreSQL, Redis) for persistence.
- JaCoCo >= 80% in `domain` and `application`.
- Spotless (google-java-format) before every commit.
- maven-enforcer for forbidden dependencies; ArchUnit for module boundaries.
