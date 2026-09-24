# HayPaComer

> Working name, subject to the branding phase. If it changes, the folder, modules, packages, and repository are renamed.

Smart home fridge that answers a daily question: **what can I cook right now with what is actually at home?**. It combines a Java application, an ESP32 module, and a kitchen scale to keep a live inventory with quantities in grams, expiry dates, food ownership, door openings, and cold chain.

## Status

Phase 0 - foundation: local git, Maven structure with 8 modules, and the original proposal moved to `docs/proposal/`. The master plan lives in [`PLAN.md`](PLAN.md).

## Stack

- Java 25 LTS + Spring Boot 4.1.1
- Maven multi-module with 8 SRP modules: `domain`, `application`, `adapter-persistence`, `adapter-sensors`, `adapter-ai`, `adapter-notifications`, `agent`, `web`
- SQLite (WAL) over JDBC; web UI with SSE
- Hardware: ESP32 + reed switch + DS18B20 + HX711 + load cell

## Documentation

- [`PLAN.md`](PLAN.md) - master plan and roadmap
- [`docs/proposal/`](docs/proposal) - original proposal (LaTeX + PDF)
