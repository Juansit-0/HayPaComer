# Step 72: Trace console and human confirmations

Commit and pull request title: `feat(agent): trace console and human confirmations`

## Goal

Every agent run can be inspected step by step, and every write the agent proposes waits for the person who asked. Approving runs the real tool once, after checking permissions again; rejecting or letting it expire closes the run without touching anything.

## Scope

- Application `agent`:
  - `PendingConfirmation` now carries its `AgentRunId` (stored as `run` in the Redis hash); `AgentRun.advance` moves status, steps, and finish time.
  - `ViewAgentRun` returns a `RunTrace` to members of the run's household (404 otherwise).
  - `ListPendingConfirmations` (the caller's, not expired).
  - `ConfirmationDesk` (record): takes a confirmation only for its author, removes it, fails the run when it expired (`ConfirmationExpiredException`), and closes runs with a trace line.
  - `RejectConfirmation`: closes the run as DONE with "Rejected by the person".
- Agent module `dev.haypacomer.agent.confirm`:
  - `ApproveConfirmation`: re-runs the `GuardrailChain` and the tool pre-check with the stored arguments, invokes the tool once, and traces "Approved: ..." (DONE) or the refusal or failure (FAILED); `ConfirmationRefusedException` when the tool is gone or a guardrail refuses.
  - `StartAgentRun`: members only, default budget.
- Web: `web` depends on `agent`; `AgentConfiguration` registers `recall_memory` and `remember`, the guardrails, and a runtime with the offline planner (LLM planning arrives with the supervisor in step 73); `AgentController`:
  - `POST /households/{h}/agent/runs` (`goal`, optional `specialist` chef, market, cold, or coach);
  - `GET /agent/runs/{id}/trace`;
  - `GET /agent/confirmations`;
  - `POST /agent/confirmations/{id}/approve` and `/reject` (404 for anyone but the author, 410 expired, 409 refused).

## Tests (definition of done)

- `TraceAndConfirmationsTest`: trace visibility, live confirmations only for their author, reject, expiry, closing a missing run.
- `ApproveConfirmationTest`: one real invocation, permissions checked again at approval, removed and failing tools traced, runs only for members.
- `AgentConsoleIntegrationTest`: a real run with memory and its four-step trace, stranger isolation, invalid specialist, approve writes memory, reject, second approve 404.
