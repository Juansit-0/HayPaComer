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
