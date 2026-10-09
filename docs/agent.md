# Agent

## Runtime loop

```text
Trigger (chat / schedule / event)
  -> AgentRuntime
       plan -> invoke tool -> observe -> done?
       |- ToolRegistry (tools = real Java services)
       |- GuardrailChain (permissions, validation, human confirmation)
       '- AgentMemory (household, preferences, decisions)
  -> response with evidence + visible trace + audit
Fallback: Circuit Breaker -> rule-based planner (offline)
```

## Runtime (step 69)

- `AgentRuntime` runs `plan -> tool -> observation` until the `Planner` returns a final answer.
- `AgentBudget`: 8 steps and 30 s by default (at most 20 steps); exhausting either ends the run as OUT_OF_BUDGET.
- WRITE tools are never invoked by the loop: the run stops in WAITING_CONFIRMATION with a `PendingConfirmation`.
- Unknown or failing tools become failed observations; a planner outage switches to `RuleBasedPlanner` and is audited as FALLBACK.

## Registry and guardrails (step 70)

- `ToolRegistry` holds each `ToolSpec` (name, kind, permission, typed parameters); `allow` narrows it per specialist.
- `GuardrailChain`: `SchemaGuardrail` (unknown, missing, malformed arguments) -> `PermissionGuardrail` (membership and role permission); a rejection is audited and returned as a failed observation.

## Tools

| Tool | Kind | Effect |
|---|---|---|
| query_inventory | read | Current inventory with quantities and zones |
| view_expiries | read | Upcoming expiries |
| weigh_now | read | Live scale reading |
| estimate_cold_window | read | Safe cold window after an incident |
| weather | read | Forecast context for suggestions |
| analytics | read | Household metrics |
| weekly_plan | read | Current weekly plan |
| register_consumption | write | Discounts measured consumption after confirmation |
| substitute_ingredient | write | Records an accepted substitution after validation |
| add_to_market | write | Adds missing items to the market list |
| create_label | write | Creates a container label |
| notify_housemates | write | Sends a notification through a channel |
| start_guided_cooking | write | Starts a guided cooking session |
| scale_recipe | write | Rescales portions after confirmation |

## Guardrails

- Reads are free; every write requires human confirmation and permission checks.
- Tools validate input and output schemas; the agent never touches the database directly.
- Per-request budgets: maximum steps, timeout, and tool allowlist.

## Memory

- Household memory: preferences, usual quantities, accepted dishes, and decisions, stored as `topic:subject` notes in `agent:memory:{householdId}`.
- Editable by the household through `/households/{h}/agent/memory` (members with COOK edit, the owner clears); memory never overrides measured inventory or rules.
- Tools: `recall_memory` (read) and `remember` (write, needs confirmation).

## Scale copilot (step 83)

- `ScaleCopilot` reacts to stable cooking readings with measured advice (add, on target, or over with the rest of the recipe rescaled) and streams it as COPILOT live updates only when the hint changes.

## Supervisor

- Routes work to lightweight specialists: Chef, Market, Cold, and Coach (`KeywordRouter`, English and Spanish, at most two per question, Chef by default).
- Each specialist runs with its own tool allowlist: Chef (memory, inventory, expiries, plan, remember), Market (market list, plan, inventory, memory, add to market), Cold (cold chain, expiries), Coach (expiries, inventory, memory, remember).
- Specialists share tools and memory; the supervisor stops at the first proposed write and merges results into one answer with evidence.

## Chef chat (step 75)

- `POST /households/{h}/agent/chat` runs the supervisor with the last 6 messages as context and returns the answer plus the observations it used.
- With an AI provider, `LlmPlanner` decides each step as JSON; an answer that quotes grams no tool observed, or cites a tool it never used, is refused and the run falls back to offline rules.

## Proactivity

- Scheduled briefings: "Today in your kitchen" from the chef at 7:00 in the household timezone, and "N foods expire soon" from the coach when 3 or more usable foods are at risk.
- Event briefings: door left open and cold chain breaches bring a cold specialist explanation, in the background, once per type and day (`agent:briefing:*` in Redis).
- A briefing never breaks the alert that triggered it, and losing Redis never stops alerts.

## Confirmations (step 72)

- A proposed write is stored with its run id; only the person who asked can approve or reject it, within 10 minutes.
- Approval checks the guardrails again with the stored arguments, runs the tool once, and closes the run (DONE or FAILED) with a trace line; rejection and expiry close it without changes.

## Trace

- Every run exposes the tool sequence, arguments, results, and confirmations.
- `AiAuditService` stores latency, valid and rejected responses, and fallback usage.
