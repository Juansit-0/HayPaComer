# HayPaComer - Master Plan v2

> Living planning document. Working name: **HayPaComer** (subject to the branding phase; if branding picks another name, the folder, modules, packages, and repository are renamed).
> Expands the original proposal (`docs/proposal/HayPaComer.tex` and `HayPaComerSlides.tex`).

- **Date**: September 2026
- **Status**: plan approved; Step 0 in progress (19 skills installed, local git and Maven structure done, CI pending)
- **Repository**: not created yet (public, after brand close with the final name)

---

## 1. Product summary

HayPaComer is a smart home fridge that answers a daily question: **"what can I cook right now with what is actually at home?"**. It combines a Java application, an ESP32 module, and a kitchen scale to keep a live inventory with quantities in grams, expiry dates, food ownership, door openings, and cold chain. AI reduces the friction of registering products and proposing substitutions, but quantities, conversions, and safety rules stay in Java and work offline.

---

## 2. What is new in v2

| Area | Change |
|---|---|
| Features | 5 new packs: Live demo, Guided cooking, Weekly plan + profiles, Household analytics, Chef chat |
| AI | Real agent layer (tools, memory, proactivity, visible trace) with multi-agent supervisor Chef/Market/Cold/Coach; 6 AI roles; hands-free voice and photo-to-recipe |
| Patterns | Full 23/23 GoF coverage, each with a real role, its own test, and documented justification |
| Brand | Professional brand phase from scratch (19 skills: 14 brand + 5 design) before naming the repository |
| Engineering | Professional stack (Java 25 LTS + Spring Boot 4.1.1 + Maven multi-module with 8 SRP modules), CI, coverage, formatting, security |
| Process | Roadmap of small steps: ~79 PRs with branch, green CI, and squash merge |

---

## 3. Features

### 3.1 Base (from the original proposal)

1. Live inventory: product, quantity, unit, zone, expiry date, and owner.
2. Cook now: available minutes, people, equipment, and restrictions; up to three real options.
3. Rescue mode: prioritizes leftovers and soon-to-expire items before suggesting purchases.
4. Collaborative shopping list: discounts what was used and groups missing items without duplicates.
5. Door sensing: opening, duration, and frequency with a threshold alert.
6. Cold chain: temperature and time out of range; flags "under review" without inventing a sanitary guarantee.
7. Two-mode scale: Fridge (real stock) and Cooking (ingredient to use).
8. Gram-based calculation: compares measured vs. required, scales portions, and computes missing amounts.
9. Verifiable substitutions: proposed equivalent weighed before acceptance.
10. Ownership and permissions: other people's food is never suggested without authorization.
11. Household learning: accepted dishes, schedules, and usual quantities.
12. Degraded operation: without WiFi or AI, inventory, alerts, and rules keep working.

### 3.2 New (approved packs)

| Pack | What it adds | Patterns it reinforces |
|---|---|---|
| **Live demo** | Real-time SSE panel, multi-channel notifications (Telegram/web/log), and fridge digital twin (visual map by zones and expiry) | Observer, Strategy |
| **Guided cooking** | Step-by-step mode with timers, guided weighing per step, and resume if interrupted | State, Builder, Mediator, Template Method |
| **Weekly plan + profiles** | 7-day "rescue first" menu and per-member profiles (allergies, diets, goals) that every suggestion respects | Strategy, Prototype, Factory Method |
| **Household analytics** | Kg saved, money avoided, per-member ranking, and monthly trend with charts | Template Method, Visitor |
| **Chef chat** | Conversational assistant anchored to the measured inventory, with real tools (not a generic chatbot) | Agent layer |

### 3.3 Reserved (not in v1)

- QR labels for containers and a leftovers handoff board between roommates.
- Installable PWA and blackout mode with a safe cold window.
- Shareable market list via link/WhatsApp and weather as suggestion context.

---

## 4. AI layer and agent

### 4.1 Six AI roles (AI is not a generic chat)

| Role | What it does | Guardrail |
|---|---|---|
| Recommender | Dish + JSON evidence (used items, grams, missing, substitutions) | Java validates quantities, profiles, and measured amounts |
| Intent parser | "Save 300 g of soup that expires Friday" -> structured command | User confirms; permissions checked |
| Label/receipt reader | Multimodal: name, quantity, date, and confidence | Preview; never saves on its own |
| Substitution advisor | Proposes a substitute from the allowed catalog | Java validates proportion, grams, and allergies |
| Status explainer | Writes "consume today" or "review this product" | Risk is computed by the cold rule, not by AI |
| Chef chat | Talks and acts with tools on the real inventory | Refuses to invent grams or safety; cites real items |

### 4.2 Agent architecture

```text
Trigger (chat / schedule / event) -> AgentRuntime
   loop: plan -> invoke tool -> observe -> done?
   |                      |- ToolRegistry (tools = real Java services)
   |                      |- GuardrailChain (permissions, validation, human confirmation)
   |                      '- AgentMemory (household, preferences, decisions)
   '- response with evidence + visible trace + audit
   Fallback: Circuit Breaker -> rule-based planner (offline)
```

- **Tools**: `query_inventory`, `view_expiries`, `weigh_now`, `register_consumption`, `scale_recipe`, `substitute_ingredient`, `add_to_market`, `create_label`, `notify_housemates`, `start_guided_cooking`, `weekly_plan`, `weather`, `estimate_cold_window`, `analytics`.
- **Free reads; writes with confirmation** and permission checks. AI never writes directly to the database.
- **Multi-agent supervisor**: routes to lightweight Chef, Market, Cold, and Coach specialists that share tools and memory.
- **Proactivity**: scheduled and event briefings (door open, high temperature, expiry clusters).
- **Extras**: hands-free voice in the kitchen (Web Speech) and photo to structured, verifiable recipe.
- **Governance**: per-case JSON contract, schema validation, step budget, timeouts, tool allowlist, no personal data, `AiAuditService` (latency, valid/rejected, fallback), and keys only in environment variables.

---

## 5. Design patterns - 23/23 GoF coverage

Honesty rule: each pattern is used in a real production flow, has a specific test, and a justified row in `docs/patterns.md`.

| Category | Patterns |
|---|---|
| Creational (5) | Singleton (`FridgeSession`), Factory Method (events, foods), Abstract Factory (ESP32 hardware vs simulated), Builder (suggestion, plan, session), Prototype (clonable templates) |
| Structural (7) | Adapter (ESP32, AI, OCR, QR), Bridge (measurement x interpretation), Composite (fridge -> zone -> tray -> food), Decorator (expired/leftover/at-risk/ownership), Facade (`HayPaComerFacade`), Flyweight (food metadata), Proxy (offline, privacy, AI cache) |
| Behavioral (11) | Chain of Responsibility (event validation), Command (auditable commands), Interpreter (units and quantities), Iterator (tree traversal), Mediator (guided cooking), Memento (undo and snapshots), Observer (panel, notifications, analytics), State (cooking and cold chain), Strategy (evaluation, channels, plan), Template Method (reports), Visitor (analytics over the composite) |

Architecture and resilience bonus: Clean Architecture, Repository, DTO, Dependency Injection, MVC, and Circuit Breaker.

---

## 6. Brand (Phase 0.5)

- **Skills installed (19, global for opencode and Claude Code)**:
  - **Brand (14)**: `brand-context`, `brand-strategy`, `brand-naming`, `brand-identity`, `brand-voice`, `brand-messaging`, `brand-guidelines`, `target-audience`, `competitor-branding`, `brand-positioning`, `brand-story`, `brand-manifesto`, `brand-measurement`, `brand-launch`.
  - **Design (5)**: `theme-factory` (palettes, fonts, themes), `design-system` (3-layer DTCG tokens), `effective-ui-design` (OKLCH, WCAG 2.1 AA, 8pt grid), `frontend-design` (anti-template aesthetic direction), `ui-ux-kit` (anti-slop gate + `DESIGN_SYSTEM.md`); plus `impeccable` (already installed) for fine UI execution.
  - Note: `design-system-generator` became obsolete in its repository; it was replaced by `design-system` from `nextlevelbuilder/ui-ux-pro-max-skill` (Socket/Snyk/Agent Trust Hub audits passed).
- **Color and branding pipeline**: context and identity -> `theme-factory` (palette and font exploration) -> `effective-ui-design` (OKLCH + WCAG contrast) -> `design-system` (3-layer tokens) -> `frontend-design` + `impeccable` (real UI) with `ui-ux-kit` as anti-slop gate.
- **Process**: context -> audience -> competition -> positioning -> strategy -> name evaluation -> identity (SVG logo, palette, typography, tokens) -> voice -> messaging -> story -> brand guidelines -> launch.
- **Deliverables** in `brand/`: full brand book, versioned `.agents/brand-context.md`, and `assets/` (logo, palette, `tokens.css`/`tokens.json` DTCG, typography, `DESIGN.md`).
- **Application**: README, repository description and topics, design tokens in the web UI (F6), and optional restyling of the slides (F7).
- **Close**: with the final name, rename what is needed and create the public repository.

---

## 7. Stack, architecture, and standards

### 7.1 Stack

| Layer | Technology |
|---|---|
| Language | Java 25 LTS |
| Framework | Spring Boot 4.1.1 |
| Build | Maven multi-module with 8 SRP modules: `domain`, `application`, `adapter-persistence`, `adapter-sensors`, `adapter-ai`, `adapter-notifications`, `agent`, `web` |
| Persistence | SQLite with WAL, indexes, and prepared statements (JDBC) |
| Web | Static UI served by Spring + SSE for real time |
| Hardware | ESP32 + reed switch + DS18B20 + HX711 + load cell + LED/buzzer |
| AI | Gemini and OpenAI-compatible endpoints, always behind `RecommendationEngine` + rules |
| Quality | JUnit 5, JaCoCo >= 80% in domain/application, Spotless, OpenAPI, Actuator, Dependabot, CI |

### 7.2 Target structure

```text
HayPaComer/
|-- .github/                  # CI + Dependabot
|-- brand/                    # Brand book + assets + design tokens
|-- docs/
|   |-- proposal/             # Original proposal and slides (tex + pdf)
|   |-- ROADMAP.md            # This plan with per-step status
|   |-- architecture.md
|   |-- patterns.md
|   |-- responsible-ai.md
|   |-- agent.md
|   |-- event-protocol.md
|   '-- adr/                  # Architecture decisions
|-- domain/                   # Domain model and business rules (pure Java, no Spring)
|-- application/              # Use cases and ports (interfaces)
|-- adapter-persistence/      # SQLite persistence
|-- adapter-sensors/          # ESP32 and event simulator
|-- adapter-ai/               # Rules, Gemini, and OpenAI-compatible + JSON contracts
|-- adapter-notifications/    # Telegram/web/log channels
|-- agent/                    # Agent runtime, tools, memory, and supervisor
|-- web/                      # Spring app: REST, SSE, UI, OpenAPI, Actuator
|-- firmware/esp32/           # reed+DS18B20 and HX711 sketches
|-- simulator/                # Event injector for hardware-free demo
'-- README.md
```

### 7.3 Quality standards

- Tests: unit tests per service and pattern, integration with temporary SQLite and MockMvc; minimum 80% coverage in the core.
- Boundaries: `maven-enforcer` forbids frameworks in `domain`/`application`; ArchUnit validates module dependencies (isolated adapters, nobody depends on `web`).
- Single responsibility: one use case per class with a single public method; ports segregated by interface (ISP); framework annotations only in `web` and adapters.
- Formatting: Spotless (google-java-format); the build fails if not formatted.
- Reliability: input validation, idempotent event ingestion, timeouts + retries with backoff, Circuit Breaker, global RFC 7807 error handler, health checks.
- Performance: WAL, indexes, prepared statements, and paginated history.
- Security: zero secrets in the repository; environment variables; strict `.gitignore`.

---

## 8. Workflow

- **Now**: local git; each completed step is a commit with Conventional Commits in English.
- **After branding**: the public repository is created with the final name and all history is pushed.
- **From the repository on**: each step goes in a `feat/*` branch -> PR -> green CI -> squash merge to `main`.
- **Commit prefixes**: `feat(scope): ...`, `fix(scope): ...`, `docs: ...`, `test: ...`, `perf: ...`, `ci: ...`, `chore: ...`.
- **Definition of Done per step**: compiles + tests + formatting + green PR.
- Deliberately small steps (~79 PRs) so the project always moves forward in green and nothing gets lost.

---

## 9. Roadmap - ~79 PRs

### F0 - Foundation (5)

1. `chore(setup): maven multi-module structure (8 srp modules) + gitignore + readme`
2. `docs(proposal): move proposal and slides to docs/`
3. `docs: roadmap, architecture, patterns, ai, agent, event protocol, and adrs`
4. `ci: build, test, and coverage pipeline + dependabot`
5. `chore(architecture): dependency enforcer + archunit boundary tests`

### F0.5 - Brand (5 + close)

6. `docs(brand): context, audience, competition, and positioning`
7. `docs(brand): strategy and name evaluation`
8. `docs(brand): identity, voice, messaging, and story`
9. `docs(brand): design system, dtcg tokens, and wcag validation`
10. `docs(brand): brand guidelines, readme application, and final assets`
11. Close: rename if needed + `gh repo create` with the final name + history push.

### F1 - Domain and persistence (7)

12. `feat(domain): quantities, units, and food metadata (flyweight)`
13. `feat(domain): fridge-zone-tray-food composite`
14. `feat(domain): iterator to traverse the tree`
15. `feat(domain): recipes, steps, requirements, and members`
16. `feat(domain): food profiles with allergies and diets`
17. `feat(domain): expired, leftover, at-risk, and ownership decorators`
18. `feat(persistence): sqlite schema, repositories, and tests`

### F2 - Application (6)

19. `feat(application): haypacomer facade (facade)`
20. `feat(application): live inventory with permissions`
21. `feat(application): collaborative market list without duplicates`
22. `feat(application): auditable inventory commands (command)`
23. `feat(application): undo and snapshots (memento)`
24. `test(application): services and business rules`

### F3 - Door and temperature sensors (8)

25. `feat(sensors): esp32 adapter and simulator (adapter)`
26. `feat(sensors): abstract factory for real and simulated hardware`
27. `feat(sensors): measurement-interpretation bridge and door alert (bridge)`
28. `feat(sensors): event validation chain (chain of responsibility)`
29. `feat(sensors): cold chain and under-review state`
30. `feat(web): rest event intake with validation and idempotency`
31. `feat(firmware): esp32 reed + ds18b20 with json events`
32. `test(sensors): noise, duplicates, and thresholds`

### F4 - HX711 scale (5)

33. `feat(scale): tare, stable reading, and calibration`
34. `feat(scale): fridge mode with measured stock discount`
35. `feat(scale): cooking mode against recipe requirement`
36. `feat(firmware): esp32 hx711 with stable reading`
37. `test(scale): tare, stability, and calibration`

### F5 - Quantities, substitutions, and guided cooking (8)

38. `feat(quantity): enough/reduce/substitute/missing evaluator (strategy)`
39. `feat(quantity): quantity and unit interpreter`
40. `feat(quantity): substitutions with proportion, limits, and allergies`
41. `feat(cooking): cooking session with states and resume (state)`
42. `feat(cooking): session, scale, and timer mediator`
43. `feat(cooking): timers and guided weighing per step`
44. `feat(quantity): automatic missing items to the market list`
45. `test(quantity): portions, substitutions, and step progression`

### F6 - AI, web, and agent (19)

46. `feat(ai): offline rule engine`
47. `feat(ai): gemini and openai-compatible adapters with validated json`
48. `feat(application): suggestion builder with constraints and profiles`
49. `feat(application): single fridge session and private food proxy`
50. `feat(planning): 7-day weekly plan with rescue-first strategy`
51. `feat(planning): clonable plan and recipe templates (prototype)`
52. `feat(planning): plan delta into the market list`
53. `feat(web): now, fridge, and market interface`
54. `feat(web): live sse panel and digital twin (observer)`
55. `feat(notifications): telegram, web, and log channels (observer, strategy)`
56. `feat(web): openapi, rfc7807 errors, and actuator health`
57. `feat(agent): plan-tool-observation runtime with budget`
58. `feat(agent): tool registry with validation and permissions`
59. `feat(agent): editable household memory`
60. `feat(agent): trace console and human confirmations`
61. `feat(agent): chef, market, cold, and coach multi-agent supervisor`
62. `feat(ai): photo to structured, verifiable recipe`
63. `feat(agent): chef chat as an agent with evidence`
64. `test(ai): response contract, weekly plan, and offline fallback`

### F7 - Analytics, robustness, and demo (16)

65. `feat(analytics): consumption, avoided waste, and money saved`
66. `feat(analytics): reports with template method and visitor`
67. `feat(web): analytics dashboard with charts and household ranking`
68. `feat(application): degraded mode with cache and retries (proxy)`
69. `feat(ai): circuit breaker and degraded responses (circuit breaker, null object)`
70. `feat(agent): proactive briefings by schedule and events`
71. `feat(agent): reactive copilot with live scale`
72. `feat(agent): market agent with budget`
73. `feat(agent): cold incident investigation`
74. `feat(agent): anti-waste coach and weekly digest`
75. `feat(agent): hands-free voice in the kitchen (web speech)`
76. `perf(persistence): indexes, wal, and inventory queries`
77. `test(integration): full demo flow and extreme noise`
78. `docs(demo): expanded demo script`
79. `docs: final readme with badges, architecture, and patterns`
80. `chore(release): v1.0.0`

---

## 10. Final demo (acceptance criteria)

1. Door open 40 s -> buzzer + Telegram notification + live SSE panel with the perishable under review.
2. Milk removal in Fridge mode: 842 g -> 650 g, stock updated to 192 g.
3. "Organize dinner and notify Ana": the supervisor splits across Chef, Market, and Coach with a visible trace of tools and grams; the user confirms.
4. Guided cooking of rice with chicken for two, with voice ("next step", "how much is left?").
5. The recipe asks for 200 g of chicken; the scale confirms 80 g -> reduce to one portion or substitute.
6. 130 g of tuna is weighed: Java validates against profile allergies, discounts, and updates the market list.
7. Photo to recipe verified against the measured inventory.
8. The AI API goes down -> Circuit Breaker -> the rule engine keeps everything working.
9. Analytics dashboard with kg saved and money avoided; v1.0.0 close.

Indicators: inventory updated, event processed, recipe respecting constraints, AI outage without system outage, and list without duplicates.

---

## 11. Recorded decisions (ADR base)

1. Stack: Java 25 LTS + Spring Boot 4.1.1 + Maven multi-module with 8 single-responsibility modules: `domain`, `application`, `adapter-persistence`, `adapter-sensors`, `adapter-ai`, `adapter-notifications`, `agent`, `web`.
2. SQLite persistence (WAL + indexes) over JDBC.
3. Web UI served by Spring + SSE; JavaFX discarded.
4. Decoupled AI: `RecommendationEngine` with offline rule engine + Gemini and OpenAI-compatible adapters with strict validated JSON.
5. Agent with validated tools: AI never writes directly to the database; multi-agent supervisor.
6. 23/23 GoF pattern coverage with test and justification.
7. Offline-first: degraded mode, Circuit Breaker, and caches.
8. Branding before the final name and the public repository.
9. Local git during foundation and branding; public repository afterwards, with branch + PR + squash flow.
10. Secrets only in environment variables (`.env`), never in the repository.
11. Single-responsibility modules enforced by `maven-enforcer` + ArchUnit (isolated adapters; nobody depends on `web`).
12. AI skills for brand and design: 19 global (14 brand + 5 design); `design-system` replaces the obsolete `design-system-generator`.
13. Project language: English for code, tests, documentation, UI, and commits; no comments in code; no emojis.

---

## 12. Next steps

- [x] Plan approved and recorded in this document
- [x] New folder created on the Desktop (`~/Desktop/HayPaComer`)
- [x] 19 skills installed (14 brand + 5 design) globally
- [x] `AGENTS.md` + `CLAUDE.md` created to resume the project from any session
- [x] Local `git init` + 8-module structure + `.gitignore` + README
- [x] Original proposal and slides moved to `docs/proposal/`
- [ ] CI + Dependabot + F0 docs (architecture, patterns, responsible AI, agent, event protocol, ADRs)
- [ ] Phase 0.5 (branding, colors, and tokens)
- [ ] Rename if needed + public repository + history push
- [ ] Phases 1-7
