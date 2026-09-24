<p align="center"><img src="brand/assets/logo-mark.svg" alt="HayPaComer" width="56"></p>

# HayPaComer

**Weigh it. Know it. Cook it.**

Smart home fridge that answers a daily question: **what can I cook right now with what is actually at home?** It combines a Java application, an ESP32 module, and a kitchen scale to keep a live inventory with quantities in grams, expiry dates, food ownership, door openings, and cold chain. Camera apps guess; HayPaComer measures. Premium fridges cost thousands; HayPaComer upgrades a normal one.

## Status

Phase 0 - foundation complete: local git, Maven structure with 8 modules, F0 docs, CI, and enforcer boundaries. Phase 0.5 - brand complete in `brand/` (strategy, identity, voice, tokens). Next: create the public repository and start Phase 1 (domain and persistence).

## Stack

- Java 25 LTS + Spring Boot 4.1.1
- Maven multi-module with 8 SRP modules: `domain`, `application`, `adapter-persistence`, `adapter-sensors`, `adapter-ai`, `adapter-notifications`, `agent`, `web`
- SQLite (WAL) over JDBC; web UI with SSE
- Hardware: ESP32 + reed switch + DS18B20 + HX711 + load cell

## Documentation

- [`PLAN.md`](PLAN.md) - master plan and roadmap
- [`brand/brand-book.md`](brand/brand-book.md) - brand guide, strategy, voice, and tokens
- [`docs/`](docs) - architecture, patterns, responsible AI, agent, event protocol, ADRs
- [`docs/proposal/`](docs/proposal) - original proposal (LaTeX + PDF)

## Demo promise

Door open 40 s -> buzzer, Telegram, and a live panel. Milk 842 g -> 650 g -> 192 g consumed, tare included. Chicken 200 g required vs 80 g measured -> reduce or weigh a substitute. AI outage -> rules keep dinner working.
