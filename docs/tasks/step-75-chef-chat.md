# Step 75: Chef chat as an agent with evidence

Commit and pull request title: `feat(agent): chef chat as an agent with evidence`

## Goal

People talk to the kitchen in their own words and get answers grounded in the real fridge. The chat is the supervisor with memory of the conversation; every answer comes with the observations it relied on, and an AI planner is never allowed to quote grams no tool measured.

## Scope

- Application:
  - port `ChatModel` (`completeJson(system, user)`) and `ChatModelUnavailableException`.
- `adapter-ai`:
  - `LlmChatModel` over Gemini or OpenAI-compatible clients (outages and invalid answers become unavailable).
- `agent`:
  - `AgentResult.evidence` (successful observations of the run);
  - `LlmPlanner` and `LlmPlannerFactory`: a prompt with the specialist purpose, its tools and parameters, and the run history; a strict JSON decision (`call` or `answer` with `evidence`);
  - the answer is refused, and the runtime falls back to offline rules, when it quotes grams not seen in an observation or cites a tool it never used;
  - `ChefChat` stores user and assistant messages, passes the last 6 messages as context to the supervisor, and only lets authors continue their conversations;
  - `ReadConversation`;
  - the supervisor ignores allowlisted tools that are not registered.
- Web:
  - `AiConfiguration` picks `OfflinePlanners` or `LlmPlannerFactory` from `AI_PROVIDER`;
  - `ChefChatController`:
    - `POST /households/{h}/agent/chat` (`message`, optional `conversationId` and `specialist`) answers `answer`, `runs`, `evidence`, and `confirmation`;
    - `GET /agent/conversations`;
    - `GET /agent/conversations/{id}`.

## Tests (definition of done)

- `LlmPlannerTest`: prompt content, calls, grounded answers, invented grams or tools refused, malformed answers, outages, clipping.
- `ChefChatTest`: evidence, conversation context, ownership, message limits.
- `LlmChatModelTest`.
- `ChefChatIntegrationTest`: real fridge evidence in Spanish, follow-up routed to market in the same conversation, history, stranger 404, empty message 400.
