# HayPaComer Brand Book

Version 1.0 - September 2026. This guide codifies every brand decision made in Phase 0.5 so anyone can apply the brand without guessing. It is the entry point; deeper detail lives in the linked documents inside `brand/`.

## Part 1 - Brand foundation

### About this guide

Audience: whoever touches HayPaComer output - code, docs, UI, README, demo, social posts. Use it as the checklist before publishing anything: if a decision here is not followed, the brand drifts. When this guide and another document disagree, this guide and `brand/assets/DESIGN.md` win.

### Brand essence

- **Mission:** Help shared and family households waste less and decide dinner from what is actually at home.
- **Vision:** Become the most trusted measured kitchen assistant in shared Latin American homes - built on honest numbers, household respect, and software that works when everything else fails.
- **Values:** Measured honesty; household respect; practical calm; resilience; frugality.
- **Personality:** Practical, approachable, honest, curious.
- **Positioning statement:** HayPaComer is the measured kitchen assistant for shared and family fridges: grams, expiry, and rules decide tonight's dinner, offline and honest.
- **Tagline:** Weigh it. Know it. Cook it.
- **Name:** HayPaComer (final; the phrase behind it is "hay pa' comer", there is food to eat).

Strategy sources: `brand/strategy.md`, `brand/positioning.md`, `brand/naming.md`.

## Part 2 - Visual identity

### Logo

**Primary mark:** the H-tray: two ink bars forming an "H" whose crossbar becomes a copper weighing tray, with a copper dot above it (the ingredient being weighed). Files: `brand/assets/logo-mark.svg` (icon) and `brand/assets/logo-lockup.svg` (mark plus wordmark).

**Variations:**

- Horizontal lockup: mark left, wordmark right (default for README, docs headers, presentations).
- Stacked lockup: mark above the three-beat wordmark (Hay / Pa' / Comer) for square spaces; use the display form "Hay Pa' Comer" only in campaign-level pieces.
- Icon only: tray mark for favicons, app icons, and hardware labels.

**Clear space:** minimum clear space equals the height of the "H" bar (1x mark unit) on every side. Nothing enters that area.

**Minimum size:** icon 24 px digital / 8 mm print; horizontal lockup 120 px wide digital / 30 mm print. Below these sizes use the icon only.

**Incorrect usage:**

- Do not stretch, condense, or rotate the mark.
- Do not add shadows, glows, gradients, or outlines.
- Do not place the lockup on busy photography without a sand container or sufficient contrast.
- Do not recolor the mark outside the approved palette (ink bars, copper tray; reversed variants use sand/white on ink).
- Do not re-set the wordmark in another typeface or add the apostrophe to running text.
- Do not animate the mark beyond a simple fade or draw-on.

### Color palette

Primaries and supporting colors (full OKLCH values and contrast validation in `brand/assets/DESIGN.md`):

| Color | Hex | Role |
|---|---|---|
| Copper Clay | `#b45309` | Primary; actions, brand moments |
| Deep Ink | `#292524` | Text, structure, mark bars |
| Sand | `#faf4ea` | Backgrounds and surfaces |
| Paprika | `#c2410c` | Attention states (door open, expiring) |
| Olive | `#4d7c0f` | Positive measured states |

Dark theme equivalents: amber `#f59e0b`, orange `#ea580c`, lime `#84cc16` on ink `#1c1917`. Print: derive CMYK from the sRGB values with the printer's profile; no Pantone match is targeted. Never use color as the only signal: status is always color plus icon plus label.

### Typography

| Level | Typeface | Weight | Size / line height |
|---|---|---|---|
| Display and wordmark | Bricolage Grotesque | 600-700 | per lockup |
| H1 | Bricolage Grotesque | 700 | 40 / 1.2 |
| H2 | Bricolage Grotesque | 700 | 32 / 1.25 |
| H3 | Hanken Grotesk | 600 | 24 / 1.33 |
| H4 | Hanken Grotesk | 600 | 20 / 1.4 |
| Body | Hanken Grotesk | 400 | 16 / 1.5 |
| Small and labels | Hanken Grotesk | 400-500 | 14 / 1.43 |
| Data (grams, dates, timers) | IBM Plex Mono | 400-600 | 16 / 1.5, tabular |

Rules: sentence case everywhere; left-aligned; 45-75 characters per line; weights limited to those listed; every measured value in the data font with tabular numerals ("842 g", "192 g used", "15 min"). Fallback stack when brand fonts are unavailable: `ui-sans-serif, system-ui, sans-serif` for text and `ui-monospace, monospace` for data. Never substitute a rounded or script font.

### Imagery and iconography

**Photography:** warm documentary realism - real kitchens, real containers, tape labels, hands weighing, the scale display in focus; natural light, warm tones, slight grain. Avoid perfect produce flat lays, showroom kitchens, stock smiling families, AI renders, neon.

**Iconography:** one SVG set (line style, 2 px stroke, slightly rounded joints), `currentColor`, always paired with a text label; icon-only buttons need an accessible name. Kitchen-specific detail is welcome (tray, scale, tag, pot); decoration is not.

## Part 3 - Verbal identity

**Voice essence:** HayPaComer sounds like a well-organized roommate who is great with numbers - direct, warm, and honest.

**Tone dimensions:** casual but not sloppy; calm; dry humor only when nothing is at risk; accessible authority; intimate but respectful.

**Core qualities:** direct, concrete, warm, honest about limits, calm in failure, respectful of ownership. Full examples, vocabulary, and channel adaptations live in `brand/voice.md`.

**Writing style essentials:** short sentences, one idea each; no exclamation marks in UI; sentence case; numerals for every measurement with units; contractions welcome; active voice by default.

**We say:** weigh, grams, tray, stock, expires, rescue, under review, enough, reduce, substitute, tonight, household, confirm, rules.
**We don't say:** revolutionize, seamless, magic, AI-powered (as a promise), smart kitchen platform, food waste warrior, guilt, hurry, oops.

## Part 4 - Messaging

**Core message:** HayPaComer helps shared and family households decide tonight's dinner from what is actually in the fridge by weighing stock, tracking expiry and cold chain, and deciding with rules that keep working offline.

**Value proposition:** an affordable scale and sensors turn a normal fridge into a measured one; ownership is respected; unlike cameras and lists, it measures instead of guessing and keeps deciding when the internet or AI does not.

**Approved tagline:** Weigh it. Know it. Cook it.

**Key messages and proof:** measured not guessed (842 g to 650 g = 192 g used); fair in a shared fridge (permission checks; deduplicated household list); rules decide safety (cold rule flags "under review"); works when everything else fails (AI outage without system outage). Full framework in `brand/messaging.md`.

## Part 5 - Brand in use

**Repository and docs:** README opens with the tagline, the measured promise, and links to `brand/` and `docs/`. Diagrams use ink lines on sand with copper highlights. Badges follow the palette, never default colors.

**Web UI:** built exclusively from `brand/assets/tokens.css`; numbers are the hero of every view; status uses dot plus label; one primary action per screen; light and dark themes are automatic.

**Social:** one number per post over a warm kitchen photo ("192 g rescued this week"), captions in the direct voice, no hashtag noise, no hype.

**Presentations and demo:** numbers as hero slides; the demo script reads like a kitchen story (door open 40 s, milk 842 to 650 g, chicken 200 vs 80 g); no stock imagery.

**Hardware:** copper-accented tray labels; small laser-etched or printed icon on the electronics enclosure; cables and sensors shown as honest hardware, never hidden as "magic".

## Part 6 - Brand don'ts

- Never use the logo below minimum size or outside approved colors.
- Never place the lockup on busy imagery without a container.
- Never use colors outside the palette, including "AI purple" gradients.
- Never set numbers in proportional figures; measured values are always tabular monospace.
- Never claim detection, freshness scores, or sanitary guarantees the system cannot prove.
- Never use guilt, urgency, or hype in copy.
- Never write "user" in customer-facing text; households, roommates, family.
- Never ship UI with hardcoded colors or spacing; tokens only.

## Part 7 - Assets and ownership

- **Brand guardian:** Juan (Juansit-0) - final approval for any new color, logo variation, or tagline.
- **Assets:** `brand/assets/` (logo files, `tokens.json`, `tokens.css`, `DESIGN.md`, `theme.md`).
- **Documentation map:** strategy in `brand/strategy.md`; audience in `brand/audience.md`; competition in `brand/competitors.md`; naming in `brand/naming.md`; identity in `brand/identity.md`; voice in `brand/voice.md`; messaging in `brand/messaging.md`; story in `brand/story.md`; context in `.agents/brand-context.md`.
- **Changes:** any brand change updates this book, the affected source doc, and the tokens in the same commit.
