# HayPaComer backend

Presentation mirror of the backend of **HayPaComer**, the smart fridge that answers "what can I cook right now with what is actually at home?". Development happens in the monorepo [Juansit-0/HayPaComer](https://github.com/Juansit-0/HayPaComer); this repository is a read-only view updated with `scripts/mirrors.sh` from that monorepo.

## What is here

The Maven multi-module backend and its tests:

| Module | Responsibility |
| --- | --- |
| `domain` | Framework-free model: fridge tree, quantities, inventory, cold chain, cooking, planning |
| `application` | Use cases and ports; one public method per use case |
| `adapter-persistence` | PostgreSQL and Redis adapters, Flyway migrations |
| `adapter-sensors` | ESP32 event decoding, hardware factories, simulators |
| `adapter-ai` | LLM clients, offline rule engine, resilience (circuit breaker) |
| `adapter-notifications` | Inbox, Telegram and log channels |
| `agent` | Framework-free agent runtime, tools, guardrails, supervisor |
| `web` | Spring Boot application, REST API, security, static UI |

The web UI is mirrored separately in `HayPaComer-web`; the migrations are also shown in `HayPaComer-db`.

## Build

Requirements: Java 25, Maven 3.9+, Docker with Compose.

```bash
cp .env.example .env
docker compose up -d
mvn verify
```

The full instructions live in the monorepo README and `docs/`.
