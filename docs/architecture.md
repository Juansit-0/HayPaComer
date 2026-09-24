# Architecture

## Context

HayPaComer is a smart home fridge: a Java backend, an ESP32 module (door, temperature, scale), a kitchen scale, and a web UI. The ESP32 measures; Java interprets events and decides.

## Modules

| Module | Responsibility | May depend on |
|---|---|---|
| domain | Model and business rules (pure Java) | nothing |
| application | Use cases and ports | domain |
| adapter-persistence | SQLite (WAL, indexes, prepared statements) | application, domain |
| adapter-sensors | ESP32 events and simulator | application, domain |
| adapter-ai | Offline rules, Gemini / OpenAI-compatible | application, domain |
| adapter-notifications | Telegram, web, log channels | application, domain |
| agent | Runtime, tools, memory, supervisor | application, domain |
| web | Spring: REST, SSE, UI, OpenAPI, Actuator | all modules |

Rules:

- No frameworks in `domain` and `application` (enforced by maven-enforcer).
- Adapters are isolated from each other; nobody depends on `web` (ArchUnit).
- Framework annotations only in `web` and adapters.
- One use case per class with a single public method; ports segregated by interface.

## Data flow

```text
ESP32 (door, temperature, weight) -> REST event intake (web)
  -> validation chain (adapter-sensors)
  -> use cases (application) -> domain rules
  -> persistence (adapter-persistence, SQLite WAL)
  -> events (Observer) -> SSE panel, notifications, analytics
```

Agent flow:

```text
Trigger -> AgentRuntime (plan, tool call, observe) -> GuardrailChain
  -> ToolRegistry (real Java services) -> response with evidence and visible trace
```

## Key mechanisms

- Idempotent event ingestion with client-generated event ids.
- Measurements are accepted only when stable; tare before weighing.
- Cold-chain risk is decided by explicit rules, never by AI.
- Offline-first: degraded mode with caches and Circuit Breaker.
- RFC 7807 error responses and a global exception handler.
- SQLite with WAL, indexes, and prepared statements.

## Quality gates

- JUnit 5 unit and integration tests; MockMvc for web; temporary SQLite for persistence.
- JaCoCo >= 80% in `domain` and `application`.
- Spotless (google-java-format) before every commit.
- maven-enforcer for forbidden dependencies; ArchUnit for module boundaries.
