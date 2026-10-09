# Step 73: Chef, market, cold, and coach multi-agent supervisor

Commit and pull request title: `feat(agent): chef, market, cold, and coach multi-agent supervisor`

## Goal

One question can need more than one kind of help ("the fridge feels warm, what do I buy?"). A supervisor routes it to small specialists that share tools and memory, each one only with the tools of its job, and merges their answers into one reply with evidence.

## Scope

- Real kitchen tools in `dev.haypacomer.agent.kitchen`, all through existing use cases:
  - reads: `query_inventory` (optional `food` filter, 40 lines max), `view_expiries` (rescue order, `days`), `view_market_list`, `view_cold_chain`, `view_weekly_plan`;
  - write: `add_to_market` (catalog food and grams, source AGENT_CONFIRMED, MANAGE_MARKET_LIST);
  - `KitchenToday` gives the date in the household timezone.
- `dev.haypacomer.agent.supervisor`:
  - `Specialist` CHEF, MARKET, COLD, COACH with a tool allowlist and offline reads;
  - `KeywordRouter` (English and Spanish stems, at most 2 specialists, CHEF by default);
  - `PlannerFactory` with `OfflinePlanners` (rule-based until an LLM planner is plugged in);
  - `Supervisor.handle` checks membership, runs each specialist with `ToolRegistry.allow`, stops at the first proposed write, and merges answers as "Market: ... / Chef: ...".
- `StartAgentRun` is replaced by the supervisor; `POST /households/{h}/agent/runs` answers `runs` (one per specialist), `answer`, and `confirmation`.
- The `application` module publishes its in-memory test fixtures as a test-jar for the `agent` tests.

## Tests (definition of done)

- `KitchenToolsTest`: every tool against the real use cases with in-memory adapters.
- `SupervisorTest`: routing in both languages, merged evidence, chosen specialist sees only its tools, a write stops the supervisor, unfinished specialists reported, strangers rejected.
- `AgentConsoleIntegrationTest`: a chef run with three reads and a ten-step trace, and a Spanish question routed to cold and market.
