# ADR 0004: Decoupled AI behind RecommendationEngine

- Status: accepted
- Date: 2026-09

## Context

AI providers change, fail, and cost money. The system must keep working without them and must never let a model decide quantities or safety.

## Decision

Define `RecommendationEngine` as the seam. Provide an offline rule-based implementation plus Gemini and OpenAI-compatible adapters that return strict JSON validated against a contract. Java validates quantities, profiles, allergies, and safety rules.

## Consequences

- The app runs fully offline with the rule engine; AI is an enhancement, not a dependency.
- Provider swaps and A/B testing are isolated in `adapter-ai`.
- Every AI response needs schema validation and fallback handling.
