# AGENTS.md - HayPaComer

> Instructions for any agent (opencode, Claude Code, Cursor, etc.) working on this project.
> Source of truth: **`PLAN.md`** - read it fully before acting.
> The original proposal is in `docs/proposal/` (LaTeX + PDF).

## What this project is

HayPaComer (working name, subject to the branding phase) is a smart home fridge: Java application + ESP32 module + kitchen scale, with live inventory, cook-now, cold chain, verifiable substitutions, and an AI agent layer with a multi-agent supervisor. It answers: "what can I cook right now with what is actually at home?".

## Current state (September 2026)

- Master plan v2 approved in `PLAN.md`: ~79 PRs, 8 SRP Maven modules, 23/23 GoF patterns, multi-AI agent, professional brand, expanded demo.
- **19 skills installed globally** (they load in any session): 14 brand (`brand-*`, `target-audience`, `competitor-branding`) + 5 design (`theme-factory`, `design-system`, `effective-ui-design`, `frontend-design`, `ui-ux-kit`) + `impeccable`.
- Local git with step commits; 8-module Maven structure; F0 docs; CI + Dependabot; enforcer + ArchUnit boundary tests. Technical Step 0 complete. Next: Phase 0.5 branding.
- **GitHub repository: intentionally NOT created yet.** It is created at brand close with the final name (`gh repo create Juansit-0/<final-name> --public`).

## How to continue (agreed order)

1. **Phase 0.5 - Branding from scratch** (next main step). Run the skills in order:
   `brand-context` -> `target-audience` -> `competitor-branding` -> `brand-positioning` -> `brand-strategy` -> `brand-naming` (evaluate/refine "HayPaComer") -> `brand-identity` -> `brand-voice` -> `brand-messaging` -> `brand-story` -> `brand-guidelines` -> `brand-launch`.
   Color pipeline: `theme-factory` (palettes and fonts) -> `effective-ui-design` (OKLCH + WCAG 2.1 AA contrast) -> `design-system` (3-layer DTCG tokens) -> `frontend-design` + `impeccable` (UI) with `ui-ux-kit` as anti-slop gate.
   Deliverables: `brand/` (brand book, assets, `tokens.css`/`tokens.json`, `DESIGN.md`) and versioned `.agents/brand-context.md`.
2. **Brand close**: rename folder/modules/packages if the name changes + create the public repository + enable branch protection (PR review required on `main`) + push all history.
3. **Technical Step 0 leftovers**: done (CI, Dependabot, F0 docs, enforcer + ArchUnit).
4. **Phases 1-7**: follow the numbered roadmap in `PLAN.md` section 9, one small step at a time.

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
