# ADR 0012: Brand and design skills for the branding phase

- Status: accepted
- Date: 2026-09

## Context

The branding phase must produce a professional identity, not ad-hoc choices: name evaluation, audience, positioning, identity, voice, messaging, and design tokens.

## Decision

Run 19 globally installed skills: 14 brand skills (`brand-context` through `brand-launch`) and 5 design skills (`theme-factory`, `design-system`, `effective-ui-design`, `frontend-design`, `ui-ux-kit`), plus `impeccable`. `design-system` replaces the obsolete `design-system-generator`.

## Consequences

- A structured, repeatable branding pipeline with versioned deliverables in `brand/`.
- The workflow depends on the skills being installed in each development machine.
- Design tokens flow from brand decisions into the web UI.
