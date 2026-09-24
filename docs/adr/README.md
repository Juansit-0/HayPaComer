# Architecture decision records

One file per decision, numbered sequentially. Status values: proposed, accepted, superseded, deprecated.

Template:

```md
# ADR NNNN: Title

- Status: accepted
- Date: YYYY-MM

## Context

What forces are at play.

## Decision

What we decided.

## Consequences

What becomes easier or harder.
```

## Index

| ADR | Title | Status |
|---|---|---|
| 0001 | Stack and module layout | accepted |
| 0002 | SQLite persistence over JDBC | accepted |
| 0003 | Web UI with SSE instead of JavaFX | accepted |
| 0004 | Decoupled AI behind `RecommendationEngine` | accepted |
| 0005 | Agent with validated tools, never direct database writes | accepted |
| 0006 | 23/23 GoF pattern coverage with tests and justification | accepted |
| 0007 | Offline-first with degraded mode and Circuit Breaker | accepted |
| 0008 | Branding before the final name and public repository | accepted |
| 0009 | Local git during foundation and branding | accepted |
| 0010 | Secrets only in environment variables | accepted |
| 0011 | Module boundaries enforced by enforcer and ArchUnit | accepted |
| 0012 | Brand and design skills for the branding phase | accepted |
| 0013 | English as the project language | accepted |
