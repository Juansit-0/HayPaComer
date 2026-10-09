# Step 71: Editable household memory

Commit and pull request title: `feat(agent): editable household memory`

## Goal

The agent remembers what the household teaches it (usual quantities, preferences, accepted dishes, decisions), and people stay in control: they can read every note, change it, or wipe it. Memory is context, never truth: it does not replace measured grams or safety rules.

## Scope

- Application `agent`: `MemoryTopic` (PREFERENCE, USUAL_QUANTITY, ACCEPTED_DISH, DECISION) and `MemoryNote` (subject normalized to lower case, 60 characters, no colon; value up to 300 characters; usual quantities must be readable by `QuantityParser`), stored under `topic:subject` in `agent:memory:{householdId}`.
- Use cases: `ViewHouseholdMemory` (any member; unreadable legacy entries are skipped), `RememberForHousehold` (COOK, at most 200 notes), `ForgetForHousehold` (COOK), `ClearHouseholdMemory` (MANAGE_HOUSEHOLD, the owner).
- Agent tools in `dev.haypacomer.agent.memory`: `recall_memory` (read) and `remember` (write: becomes a confirmation, validated before it is proposed through the new `AgentTool.problem` hook).
- Web: `HouseholdMemoryController` on `/households/{h}/agent/memory` (GET, PATCH with `forget` and `remember`, DELETE) and `HouseholdMemoryConfiguration`.

## Tests (definition of done)

- `HouseholdMemoryTest`: edit, normalize, skip legacy keys, guests read only, strangers get 404, owner-only clear, the 200-note ceiling, validation.
- `MemoryToolsTest`: recall through the offline planner, `remember` waits for confirmation and writes only when invoked, invalid notes never become proposals.
- `HouseholdMemoryIntegrationTest`: PATCH, forget, 422 unreadable quantity, 400 missing value, 404 stranger, 401 anonymous, DELETE.
