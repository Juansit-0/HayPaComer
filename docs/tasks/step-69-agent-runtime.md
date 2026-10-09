# Step 69: Plan-tool-observation agent runtime with budget

Commit and pull request title: `feat(agent): plan-tool-observation runtime with budget`

## Goal

The agent works in a loop that anyone can audit: it plans, calls one tool, reads what the tool observed, and either keeps going or answers. Every run has a step budget and a timeout, every step lands in the visible trace, and writes never run on their own: they wait for a person.

## Scope

- `agent` module (framework-free, enforcer bans Spring, Jakarta, and Hibernate; JaCoCo 80%), package `dev.haypacomer.agent.runtime`:
  - `AgentTool` (name, `ToolKind` READ or WRITE, `describe`, `invoke`) with `ToolInvocation` and `Observation` (4000 characters max, failed flag).
  - `Planner` returns a sealed `Decision`: `CallTool` (tool, arguments, reason) or `FinalAnswer`; it sees `AgentContext` (task, tool names, history of `Exchange`, steps left). `PlannerUnavailableException` signals a provider outage.
  - `AgentBudget` (1-20 steps, positive timeout; default 8 steps and 30 s) and `AgentTask` (household, user, specialist, goal).
  - `AgentRuntime.run(task, budget)` -> `AgentResult`:
    - saves the `AgentRun` and traces PLAN, TOOL_CALL, OBSERVATION, and ANSWER through `AgentRunStore`;
    - unknown tools and tool exceptions become failed observations the planner can react to;
    - a WRITE tool is never invoked: the runtime proposes a `PendingConfirmation` and stops in WAITING_CONFIRMATION;
    - OUT_OF_BUDGET when the steps or the timeout run out;
    - when the planner is unavailable it records FALLBACK in `AiAuditLog` and continues with `RuleBasedPlanner` (offline reads in a fixed order, then an answer with only measured evidence); FAILED if no planner works.
- Tools and their validation and permissions come in step 70; Spring wiring comes with the first real tools.

## Tests (definition of done)

- `AgentRuntimeTest`: full loop with trace order, step budget, timeout, write proposal without invocation, unknown and failing tools, offline fallback with audit, no planner, input validation.
- `RuleBasedPlannerTest`: reads each registered tool once in order and answers without inventing evidence.
