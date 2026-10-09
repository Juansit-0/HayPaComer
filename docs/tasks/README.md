# Task guides

Guides for roadmap steps assigned to a teammate. Each guide explains the goal, the code to reuse, the expected scope, the tests, and the definition of done. The design inside each guide is a starting point: if you find a better shape that keeps the rules in `CONTRIBUTING.md`, use it and explain it in the pull request.

| Step | Guide | Owner | Depends on |
|---|---|---|---|
| 61 | [7-day weekly plan with rescue-first strategy](step-61-weekly-plan.md) | Jenifer Urbano (`Jenifrutica`) | F5 merged |
| 62 | [Clonable plan and recipe templates (prototype)](step-62-templates-prototype.md) | Jenifer Urbano (`Jenifrutica`) | 61 |
| 63 | [Plan delta into the market list](step-63-plan-delta-market.md) | Jenifer Urbano (`Jenifrutica`) | 61 |
| 66 | [Telegram, web, and log channels (Observer, Strategy)](step-66-notifications.md) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 67 | [OpenAPI catalog, RFC 7807 errors, and actuator health](step-67-openapi-problems-health.md) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 68 | [Redis stores for agent memory, conversations, traces, and confirmations](step-68-agent-redis-stores.md) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 69 | [Plan-tool-observation agent runtime with budget](step-69-agent-runtime.md) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 70 | [Agent tool registry with validation and permissions](step-70-agent-tool-registry.md) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 71 | [Editable household memory](step-71-household-memory.md) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 72 | [Trace console and human confirmations](step-72-trace-console-confirmations.md) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 73 | Chef, market, cold, and coach multi-agent supervisor (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 74 | Photo to structured, verifiable recipe (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 75 | Chef chat as an agent with evidence (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 76 | Response contract, weekly plan, and offline fallback tests (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 77 | Consumption, avoided waste, and money saved (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 78 | Reports (Template Method, Visitor) (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 79 | Analytics dashboard with charts and household ranking (frontend) (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 80 | Degraded mode with cache and retries (Proxy) (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 81 | Circuit breaker and degraded responses (Null Object) (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 82 | Proactive briefings by schedule and events (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 83 | Reactive copilot with live scale (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 84 | Market agent with budget (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 85 | Cold incident investigation (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 86 | Anti-waste coach and weekly digest (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 87 | Hands-free voice in the kitchen with Web Speech (frontend) (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 88 | Indexes, partitions, and inventory queries (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 89 | Full demo flow and extreme noise tests (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 90 | Expanded demo script (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 91 | Final README with badges, architecture, and patterns (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |
| 92 | Release v1.0.0 (guide when the step starts) | Jenifer Urbano (`Jenifrutica`) | previous steps of its phase |

Steps 56 to 60 and the web interface steps 64 and 65 stay with the owner (Juan Camilo Lopez Diaz). Each step above gets a full guide (goal, code to reuse, scope, tests, docs) when it is about to start.

## Working in parallel without conflicts

- Flyway: use the next free `V<n>` number when you open the pull request. If `main` takes that number first, rename your file to the next one before merging.
- Spring wiring: add your beans in a new `web/src/main/java/dev/haypacomer/web/planning/PlanningConfiguration.java` instead of editing `KitchenConfiguration`.
- Rebase on `main` (`git pull --rebase origin main`) before asking for the merge.
