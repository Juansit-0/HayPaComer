# AGENTS.md - HayPaComer

> Instructions for any agent (opencode, Claude Code, Cursor, etc.) working on this project.
> Source of truth: **`PLAN.md`** - read it fully before acting.
> The original proposal is in `docs/proposal/` (LaTeX + PDF).

## What this project is

HayPaComer (name confirmed in Phase 0.5) is a smart home fridge: Java application + ESP32 module + kitchen scale, with live inventory, cook-now, cold chain, verifiable substitutions, and an AI agent layer with a multi-agent supervisor. It answers: "what can I cook right now with what is actually at home?".

## Current state (September 2026)

- Master plan v2 approved in `PLAN.md`: ~79 PRs, 8 SRP Maven modules, 23/23 GoF patterns, multi-AI agent, professional brand, expanded demo.
- **19 skills installed globally** (they load in any session): 14 brand (`brand-*`, `target-audience`, `competitor-branding`) + 5 design (`theme-factory`, `design-system`, `effective-ui-design`, `frontend-design`, `ui-ux-kit`) + `impeccable`.
- Local git with step commits; 8-module Maven structure; F0 docs; CI + Dependabot; enforcer + ArchUnit boundary tests. Technical Step 0 complete.
- Brand phase (F0.5) complete in `brand/`: context, audience, competitors, positioning, strategy, naming (name confirmed: **HayPaComer**), identity, voice, messaging, story, launch plan, brand book, and design tokens in `brand/assets/`. Brand close complete: public repository with branch protection.
- **Public repository: https://github.com/Juansit-0/HayPaComer** with `main` protected: PR required, 1 review approval (collaborator Jenifrutica), CI check `build` required, admins enforced.

## Where we left off

- Date: 2026-09-24. All Phase 0 (foundation) and Phase 0.5 (brand) work is merged on `main`; CI green; 24 commits of history.
- Pending: collaborator **Jenifrutica must accept the repository invite** before any PR can be approved.
- Open: five Dependabot PRs (#1 to #5: JUnit 6.1.3, enforcer 3.6.3, ArchUnit 1.5.0, actions/checkout v7, actions/setup-java v6). Review carefully; the JUnit 6 bump is a major version.
- Hardware for the demo: bill of materials with MercadoLibre Colombia links in `docs/hardware.md`.
- **Next action:** Phase 1, step 12: `feat(domain): quantities, units, and food metadata (flyweight)` in a `feat/*` branch, PR, green CI, and approval by Jenifrutica.

## How to continue (agreed order)

1. **Phases 1-7**: follow the numbered roadmap in `PLAN.md` section 9, one small step at a time, each in a `feat/*` branch with PR, green CI, and review approval by the collaborator.

## Project rules (non-negotiable)

- **English everywhere**: code, identifiers, tests, documentation, UI, and commits. No emojis. No comments in code.
- **Conventional Commits in English** with scope; small steps. Once the repository exists: `feat/*` branch -> PR -> green CI -> review approval -> squash merge (branch protection on `main`). Before the repository: local commits on `main`.
- **8 single-responsibility modules**: `domain`, `application`, `adapter-persistence`, `adapter-sensors`, `adapter-ai`, `adapter-notifications`, `agent`, `web`. No frameworks in `domain`/`application`; boundaries enforced with `maven-enforcer` + ArchUnit.
- One use case per class with a single public method; segregated ports (ISP); framework annotations only in `web` and adapters.
- **AI never writes directly to the database**: it acts through validated tools with human confirmation and permissions; grams are measured, safety is decided by rules.
- Secrets only in environment variables (`.env`); strict `.gitignore`; never keys in the repository.
- Tests with JUnit 5, JaCoCo coverage >= 80% in `domain`/`application`, Spotless (google-java-format) before every commit.

## Useful commands

```bash
npx skills ls -g
npx skills update -g
mvn -q verify
```
