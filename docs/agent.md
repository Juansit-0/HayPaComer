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

- Household memory: preferences, usual quantities, accepted dishes, and decisions.
- Editable by the user; memory never overrides measured inventory or rules.

## Supervisor

- Routes work to lightweight specialists: Chef, Market, Cold, and Coach.
- Specialists share tools and memory; the supervisor merges results into one answer with evidence.

## Proactivity

- Scheduled briefings (daily plan, expiring soon).
- Event briefings: door open, temperature out of range, expiry clusters.

## Trace

- Every run exposes the tool sequence, arguments, results, and confirmations.
- `AiAuditService` stores latency, valid and rejected responses, and fallback usage.
