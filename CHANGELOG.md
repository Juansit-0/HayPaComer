# Changelog

All notable changes to HayPaComer. Each step of the roadmap was one pull request; the full list is in [`docs/ROADMAP.md`](docs/ROADMAP.md).

## [1.0.0] - 2026-10-09

First complete release: the acceptance demo in `PLAN.md` section 10 runs end to end.

### Foundation and brand (F0, F0.5, F0.9)

- 8 single-responsibility Maven modules with enforcer and ArchUnit boundaries, CI, and Dependabot.
- Brand "Copper Counter": strategy, identity, voice, and design tokens with automatic light and dark themes.
- Data model, REST catalog, JWT authentication, PostgreSQL with Flyway, and Redis for AI state.

### Domain, persistence, and accounts (F1, F1.5, F2)

- Fridge composite with zones, trays, and food in grams; food metadata flyweight; freshness, at-risk, leftover, and ownership decorators.
- Households with roles and permissions, invitations, email verification, password reset, refresh token rotation with reuse detection, and device keys.
- Inventory as audited commands with idempotency, undo, and snapshots; market list; private, ask-first, and shared food with grants.

### Sensors and scale (F3, F4)

- ESP32 door and temperature firmware and HX711 scale firmware with buffered, batched uploads.
- Sensor validation chain, fridge monitor with one alert per episode, cold chain states with human review, and the buzzer and LED commands.
- Scale calibration, stability detection, and automatic consumption when food leaves the scale.

### Cooking (F5)

- Recipe evaluation with strict, flexible, and rescue strategies; a quantity interpreter in English and Spanish; substitutions with proportion, limits, and allergies.
- Guided cooking sessions with states, timers, weighing targets, and missing items to the market list.

### AI, web, and agent (F6)

- Offline rule engine plus Gemini and OpenAI-compatible adapters with strict JSON contracts, response cache, rate limits, and circuit breaker.
- Weekly rescue-first plan, clonable templates, and plan delta to the market list.
- Web UI with Now, Fridge, Market, live SSE panel, and digital twin; notifications by web, Telegram, and log; OpenAPI and RFC 7807 errors.
- Agent:
  - runtime with step and time budgets, tool registry with guardrails, and editable household memory;
  - trace console and human confirmations;
  - chef, market, cold, and coach supervisor;
  - photo to verified recipe and chef chat with evidence.

### Analytics, robustness, and demo (F7)

- Analytics of consumption, rescued food, waste, and money with household prices; CSV and Markdown reports; the Numbers dashboard with ranking.
- Degraded mode for reference data; one circuit breaker per AI provider with a Null Object chat model.
- Proactive briefings; scale copilot; market budget; cold incident investigation; anti-waste coach and weekly digest.
- Hands-free voice in the Chef screen.
- One-join fridge loading, batched saves, and monthly partitions for sensor events.
- Full demo and extreme noise integration tests, which found and fixed lost updates under concurrent use with a per-household lock.
- Demo script with seed and sensor tools, and the final README.
