# DESIGN.md - HayPaComer Design System (Copper Counter)

Single source of truth for the visual language of HayPaComer. Built from `brand/identity.md`, validated against WCAG 2.1 AA, and consumed as tokens by the web UI in later phases. Everything in the product references these tokens; raw hex values never appear in components.

## Files

| File | Purpose |
|---|---|
| `brand/assets/tokens.json` | Design tokens in DTCG format, three layers: primitive, semantic, component |
| `brand/assets/tokens.css` | CSS custom properties with automatic light and dark themes (`light-dark()`) |
| `brand/assets/theme.md` | Theme summary in the theme-factory format |
| `brand/identity.md` | Visual identity brief this system implements |

## Design principles

- **Numbers first:** grams, dates, and minutes are the largest elements on any screen.
- **Tool, not toy:** controls look touchable and predictable; no animation for decoration.
- **Warm utility:** sand surfaces and copper accents keep function humane.
- **Honest states:** unknown, stale, and under review are shown plainly, never hidden.

## Color

### Primitives

| Token | Hex | OKLCH | Role |
|---|---|---|---|
| `--color-copper-500` | `#c2711c` | `oklch(62.6% 0.136 60.3)` | Copper highlight, large-text only |
| `--color-copper-600` | `#b45309` | `oklch(55.5% 0.146 49.0)` | Primary (light theme) |
| `--color-copper-700` | `#9a3412` | `oklch(47.0% 0.143 37.3)` | Primary hover |
| `--color-copper-800` | `#7c2d12` | `oklch(40.8% 0.116 38.2)` | Primary active |
| `--color-paprika-600` | `#c2410c` | `oklch(55.3% 0.174 38.4)` | Attention (light theme) |
| `--color-olive-600` | `#4d7c0f` | `oklch(53.2% 0.141 131.6)` | Positive (light theme) |
| `--color-olive-700` | `#3f6212` | `oklch(45.3% 0.113 130.9)` | Positive pressed |
| `--color-amber-500` | `#f59e0b` | `oklch(76.9% 0.165 70.1)` | Primary (dark theme) |
| `--color-orange-600` | `#ea580c` | `oklch(64.6% 0.194 41.1)` | Attention (dark theme) |
| `--color-lime-500` | `#84cc16` | `oklch(76.8% 0.204 130.8)` | Positive (dark theme) |
| `--color-ink-950` | `#1c1917` | `oklch(21.6% 0.006 56.0)` | Dark background |
| `--color-ink-900` | `#292524` | `oklch(26.8% 0.006 34.3)` | Text (light), dark surface |
| `--color-ink-600` | `#57534e` | `oklch(44.4% 0.010 73.6)` | Weak text (light), dark border |
| `--color-ink-500` | `#78716c` | `oklch(55.3% 0.012 58.1)` | Strong border (light) |
| `--color-ink-400` | `#a8a29e` | `oklch(71.6% 0.009 56.3)` | Strong border (dark) |
| `--color-sand-50` | `#faf4ea` | `oklch(96.9% 0.015 80.7)` | Light background |
| `--color-sand-100` | `#f5edde` | `oklch(94.8% 0.022 83.3)` | Light surface |
| `--color-sand-200` | `#eadfc8` | `oklch(90.6% 0.033 85.5)` | Decorative border (light) |
| `--color-sand-300` | `#d6ccbb` | `oklch(84.9% 0.026 81.1)` | Weak text (dark) |
| `--color-white` | `#ffffff` | `oklch(100% 0 0)` | Foreground on primary and attention |

### Semantic mapping

| Token | Light | Dark |
|---|---|---|
| `--color-bg` | sand-50 | ink-950 |
| `--color-surface` | sand-100 | ink-900 |
| `--color-text` | ink-900 | sand-50 |
| `--color-text-weak` | ink-600 | sand-300 |
| `--color-border` | sand-200 | ink-600 |
| `--color-border-strong` | ink-500 | ink-400 |
| `--color-primary` | copper-600 | amber-500 |
| `--color-primary-fg` | white | ink-950 |
| `--color-attention` | paprika-600 | orange-600 |
| `--color-attention-fg` | white | ink-950 |
| `--color-positive` | olive-600 | lime-500 |
| `--color-positive-fg` | white | ink-950 |
| `--color-focus` | copper-600 | amber-500 |

Theme switching is automatic through `color-scheme: light dark` and `light-dark()`; no duplicated media queries, no manual toggle required. A manual toggle may override `color-scheme` later.

### Contrast validation (WCAG 2.1 AA)

| Pair | Ratio | Result |
|---|---|---|
| text on bg (light) | 13.86:1 | AAA |
| text-weak on bg (light) | 6.97:1 | AA |
| primary as text on bg (light) | 4.59:1 | AA |
| attention as text on bg (light) | 4.73:1 | AA |
| positive as text on bg (light) | 4.56:1 | AA |
| border-strong on bg (light) | 4.38:1 | UI component (>= 3:1) |
| primary-fg on primary (light) | 5.02:1 | AA |
| attention-fg on attention (light) | 5.18:1 | AA |
| positive-fg on positive (light) | 4.99:1 | AA |
| text on bg (dark) | 15.98:1 | AAA |
| text-weak on bg (dark) | 11.00:1 | AAA |
| primary as text on bg (dark) | 8.14:1 | AAA |
| attention as text on bg (dark) | 4.91:1 | AA |
| positive as text on bg (dark) | 8.85:1 | AAA |
| border-strong on bg (dark) | 6.93:1 | UI component (>= 3:1) |
| primary-fg on primary (dark) | 8.14:1 | AAA |
| attention-fg on attention (dark) | 4.91:1 | AA |
| positive-fg on positive (dark) | 8.85:1 | AAA |

`--color-copper-500` is reserved for large text and decorative use at 3:1+; it is never body text on sand. Color is never the only indicator: every status pairs a token color with an icon and a text label.

## Typography

| Role | Family | Size / line height | Weight |
|---|---|---|---|
| Display, wordmark | Bricolage Grotesque | per lockup | 600-700 |
| Heading 1 | Bricolage Grotesque | 40 / 1.2 | 700 |
| Heading 2 | Bricolage Grotesque | 32 / 1.25 | 700 |
| Heading 3 | Hanken Grotesk | 24 / 1.33 | 600 |
| Heading 4 | Hanken Grotesk | 20 / 1.4 | 600 |
| Body | Hanken Grotesk | 16 / 1.5 | 400 |
| Small, labels | Hanken Grotesk | 14 / 1.43 | 400-500 |
| Data, grams, dates, timers | IBM Plex Mono | 16 / 1.5, tabular numerals | 400-600 |

Rules: sentence case everywhere; left-aligned; line length 45 to 75 characters; weights limited to 400/500/600/700; every measured value uses the data font with `font-variant-numeric: tabular-nums`. Fonts are open source (Google Fonts); self-host in the web module before production.

## Spacing, radius, elevation, motion

- **Spacing (8pt grid):** `--space-xs` 8, `--space-s` 16, `--space-m` 24, `--space-l` 32, `--space-xl` 48, `--space-xxl` 80.
- **Radius:** `--radius-6` small details, `--radius-10` controls, `--radius-14` cards, `--radius-full` chips and status pills.
- **Elevation:** `--shadow-subtle` for cards; `--shadow-raised` for overlays and menus. No glow, no colored shadows.
- **Motion:** `--duration-fast` 120ms for state feedback, `--duration-base` 200ms for enter or expand, `--ease-standard` for all easing. `prefers-reduced-motion` sets durations to zero.

## Component specifications

### Buttons

| Property | Primary | Secondary | Ghost |
|---|---|---|---|
| Background | `--button-primary-bg` | `--button-secondary-bg` | transparent |
| Text | `--button-primary-fg` | `--button-secondary-fg` | `--button-ghost-fg` |
| Border | none | 1px `--button-secondary-border` | none |
| Hover bg | `--button-primary-bg-hover` | `--button-secondary-bg-hover` | `--color-surface` |
| Active bg | `--button-primary-bg-active` | `--color-surface` | `--color-surface` |
| Radius | `--button-radius` | `--button-radius` | `--button-radius` |

Sizes: minimum 48x48 px target; padding 12px 20px; font 16/500; label is verb + noun ("Weigh ingredient", "Confirm substitution"). One primary button per screen. Disabled buttons are avoided: validate on submit and show inline errors instead. Focus is always visible: 2px `--color-focus` outline with 2px offset.

### Inputs

Single column, label above the field, hint above the field as well. Field width matches expected input (gram amounts are narrow, names are wide). Border 1px `--input-border`; focus ring 2px `--input-focus-ring`; placeholder uses `--input-placeholder`. Validation appears after interaction using `:user-valid` and `:user-invalid`.

### Cards

Background `--card-bg`, border 1px `--card-border`, radius `--card-radius`, padding `--card-padding`, shadow `--shadow-subtle`. Cards group a measurement with its action: value, unit, expiry, owner, and one primary action per card.

### Badges and status

Badges use `--badge-*` tokens with small text. Status is a dot plus label: `--status-attention` with the label "under review" or "expiring", `--status-positive` with "fresh" or "enough". Never color alone.

### Data display

The data-display component renders the primary measurement of a view: data font, `--data-size` or larger (up to `--size-h2` for hero numbers), tabular numerals, unit separated by a thin space ("842 g", "192 g used", "15 min").

## Accessibility checklist (current status)

- All text pairs >= 4.5:1 and UI borders >= 3:1: validated above.
- Color is never the only indicator: dot plus label for status.
- Targets >= 48x48 px: enforced in button and input specs.
- Semantic HTML, skip link, and landmarks: required in the web module (F6).
- Zoom is never disabled; viewport meta allows scaling.
- Motion respects `prefers-reduced-motion` through token overrides.
- Focus visible on every interactive element at 2px with offset.
- Icons are SVG with `currentColor` and always paired with text labels.

## Usage rules

- Use tokens only: `var(--color-primary)`, `var(--space-m)`, `var(--data-font)`. Hardcoded hex values and magic pixel values are rejected in review.
- Themes are automatic: use semantic and component tokens so light and dark both work without conditionals.
- One primary action per screen; the most important action sits first, left-aligned.
- Numbers are heroes: if a measurement is not the largest element of its view, redesign the view.
- New colors require a new primitive with OKLCH conversion and a contrast check before use.
