# ADR 0005: Agent with validated tools, never direct database writes

- Status: accepted
- Date: 2026-09

## Context

An LLM with direct data access can corrupt inventory, bypass permissions, or invent unsafe suggestions. The product promise is verifiable actions on a measured inventory.

## Decision

The agent acts only through registered tools that wrap real Java services. Reads are free; every write requires schema validation, permission checks, and human confirmation. A supervisor routes work to Chef, Market, Cold, and Coach specialists that share tools and memory.

## Consequences

- Every action is auditable and reproducible outside the model.
- Tool contracts become the safety boundary and are tested like any service.
- Slightly slower flows because writes need confirmation.
