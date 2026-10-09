# Usability and accessibility pass (plan v2, step B7)

Checked on 2026-10-09 in the browser against the local app with axe-core 4.10 (rules WCAG 2.0 A and AA, WCAG 2.1 AA, and best practices), keyboard only, and phone and desktop sizes, in light and dark themes.

## Screens checked

Sign in, Now, Fridge with the add food sheet, Market, Numbers, Chef, and Settings, each at 1280 px and 375 px wide, in light and dark.

## Found and fixed

| Problem | Where | Fix |
|---|---|---|
| Copper text 4.31:1 on the tile surface (needs 4.5:1) | Links, ghost buttons, "Change the budget" in light theme | New token `--color-primary-text` (copper-700 `#9a3412`, 6.28:1); primary buttons keep copper-600 with white text (5.02:1) |
| Attention text 4.45:1 on the tile surface | "Expiring" status in light theme | `--color-attention` light is paprika-700 `#b23a0b` (5.15:1; white on it 5.99:1) |
| Attention text 4.26:1 on the dark surface | "Expiring" status in dark theme | `--color-attention` dark is orange-500 `#f97316` |
| Buttons kept the old theme color after the system theme changed (3.48:1) | Every primary button when switching light and dark with the page open | Removed the background transition on buttons; hover is instant |
| A `header` inside the sheet counted as a second banner landmark | Add food sheet | The sheet heading row is a `div` |

## Confirmed

- No axe violations on any screen in light or dark after the fixes.
- Keyboard order on sign in: skip link, home, language, email, password, sign in; focus is always visible (2 px ring with offset).
- The add food sheet moves focus inside when it opens and returns it to "Add to fridge" when Escape closes it.
- No horizontal scroll at 375 px, and no button, input, or chip under 44 px.
- Every visible text comes from the translations (`WebTextsTest`), so screen readers read the chosen language; `html lang` follows it.
