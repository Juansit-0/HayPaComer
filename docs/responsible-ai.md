# Responsible AI

## Principles

1. AI is replaceable, not critical: without AI the system keeps inventory, alerts, scale, and rule-based suggestions.
2. AI never writes directly to the database: it acts through validated tools, with human confirmation and permission checks.
3. Grams are measured, not generated: quantities come from the scale or from the user.
4. Safety is decided by rules: cold-chain risk and food safety come from explicit rules, never from a model.
5. No unnecessary personal data: the AI receives the minimum inventory context.
6. Keys live only in environment variables; never in the repository.

## AI roles and guardrails

| Role | What it does | Guardrail |
|---|---|---|
| Recommender | Dish + JSON evidence: used items, grams, missing, substitutions | Java validates quantities, profiles, and measured amounts |
| Intent parser | Turns "save 300 g of soup that expires Friday" into a structured command | User confirms; permissions checked |
| Label and receipt reader | Multimodal extraction: name, quantity, date, confidence | Preview first; never saves on its own |
| Substitution advisor | Proposes a substitute from the allowed catalog | Java validates proportion, grams, and allergies |
| Status explainer | Writes "consume today" or "review this product" | Risk level comes from the cold-chain rule |
| Chef chat | Converses and acts through tools on the real inventory | Refuses to invent grams or safety; cites real items |

## Governance

- Per-case JSON contract with schema validation; invalid responses are rejected and audited.
- Step budget, timeouts, and tool allowlist per request.
- `AiAuditService` records latency, valid and rejected responses, and fallback usage.
- Circuit Breaker falls back to the rule-based planner when the provider is down or degraded.

## Forbidden

- Inventing quantities, expiry dates, or sanitary guarantees.
- Writing to the database or running destructive actions without confirmation.
- Suggesting food without checking ownership and permissions.
- Sending personal data that the task does not require.
