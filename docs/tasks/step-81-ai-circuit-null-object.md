# Step 81: Circuit breaker and degraded responses (Circuit Breaker, Null Object)

Commit and pull request title: `feat(ai): circuit breaker and degraded responses (circuit breaker, null object)`

## Goal

When the AI provider is down, every AI feature should notice once, stop waiting on timeouts, and answer in a degraded but useful way: suggestions and the chef chat from the offline rules, recipe photos with a clear "try again in a minute". People and operators should be able to see that the AI is resting.

## Scope

- `adapter-ai` `resilience`:
  - `ProviderCircuit`: the circuit breaker extracted from `ResilientKitchenAdvisor`; one state per provider in Redis (`ai:cb:{provider}`), so suggestions, chat, and photos share it; only outages count; when open or opening it marks `ai provider <name>` degraded in `ServiceHealth`, and recovers it on the next success.
  - `ResilientKitchenAdvisor` now delegates to `ProviderCircuit`.
  - `ResilientChatModel`: chat planning behind the circuit; when open it answers with the Null Object.
  - `ResilientRecipePhotoReader`: when open, answers 503 right away without calling the provider; unreadable photos are not outages.
  - Null Object `NullChatModel`: a chat model that never fails and always answers `{"action":"defer"}`.
- Agent:
  - new `Decision.Defer` (from the Null Object or a model with nothing to add);
  - the runtime hands over to the offline rules without counting an error, traces "deferred", and audits FALLBACK;
  - a deferring fallback fails the run.
- Application: `PhotoReadingUnavailableException.providerDown` separates outages from unreadable photos.
- Web: `AiConfiguration` builds one circuit per provider for the advisor, the planner model, and the photo reader; `/api/v1/status` lists the AI provider while it rests.

## Tests (definition of done)

- `AiCircuitTest`:
  - the chat model opens the circuit and the Null Object defers instantly;
  - recovery after the open window closes it and clears the status;
  - non-outage errors are not counted;
  - photo reading short-circuits while resting, and unreadable photos do not count.
- `ResilientKitchenAdvisorTest` (through `ProviderCircuit`), `LlmPlannerTest` (defer), `AgentRuntimeTest` (defer hands over, deferring fallback fails), `AiConfigurationTest` (every feature wired to the circuit or offline).
