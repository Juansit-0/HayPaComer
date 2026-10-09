# Step 70: Agent tool registry with validation and permissions

Commit and pull request title: `feat(agent): tool registry with validation and permissions`

## Goal

The agent can only reach the household through tools it knows, with arguments that follow a schema, and only when the person behind the run is allowed to do that action. A model that invents a tool, sends grams as "a lot", or asks for a write the member's role does not allow gets a rejection it can read, never a side effect.

## Scope

- `dev.haypacomer.agent.tools`:
  - `ToolSpec` (lower snake case name, description, READ or WRITE, required `Permission`, parameters); reads need `VIEW_HOUSEHOLD`, writes need something more.
  - `ParameterSpec` and `ParameterType` (TEXT up to 200 characters, GRAMS 1-100000, COUNT 1-50, ISO DATE, ID).
  - `ToolRegistry`: unique names, registration order for planner prompts, `allow(allowlist)` narrows the tools of a specialist and fails on unknown names.
  - Chain of Responsibility `GuardrailChain` over `Guardrail`: `SchemaGuardrail` (unknown, missing, or malformed arguments) then `PermissionGuardrail` (member of the household and `Household.can` for the tool permission).
- `AgentTool` now exposes its `ToolSpec`; `AgentRuntime` takes a `ToolRegistry` and a `GuardrailChain`, checks every call before reading or proposing a write, audits rejections as REJECTED, and returns the reason as a failed observation.

## Tests (definition of done)

- `ToolRegistryTest`: order, allowlists, duplicates, malformed specs, every parameter type, the chain stopping at the first rejection.
- `AgentRuntimeTest`: bad arguments rejected without a confirmation, a guest cannot propose a market write, a stranger cannot read.
