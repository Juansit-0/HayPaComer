# Step 79: Analytics dashboard with charts and household ranking

Commit and pull request title: `feat(web): analytics dashboard with charts and household ranking`

## Goal

Give the household a screen that answers "what did our fridge save?" at a glance: the grams rescued first, then money, waste, a day-by-day picture, and who in the house rescued the most.

## Scope

- Web UI, new "Numbers" tab (`#/numbers`, `app/analytics.js`), following the Copper Counter design system (numbers first, tokens only, light and dark automatic, bottom tabs on phones):
  - period switch for 7, 30, or 90 days in the browser's local date;
  - hero number for grams rescued before expiring, then money saved, grams eaten, grams thrown away with the waste percentage, and money lost, in the household currency (`Intl.NumberFormat`);
  - diverging bar chart as plain SVG (no chart library): eaten above the line with the rescued part highlighted, thrown away below; a visually hidden table gives the same numbers to screen readers;
  - ranking of members by rescued grams with meters and a "You" marker;
  - top foods, with older movements recorded before step 77 shown as "Not recorded (older moves)";
  - price form (prefilled with the first unpriced food), and report downloads (Markdown and CSV) fetched with the session token;
  - honest empty state when no food left the fridge in the period.
- Backend: `HouseholdMemberNames` use case, shared with `ExportHouseholdReport`; `/analytics` members now include `name`.

## Tests (definition of done)

- `AnalyticsIntegrationTest` checks member names; `StaticUiIntegrationTest` serves `analytics.js` and the Numbers tab.
- Checked by hand in the browser against the real jar and Docker databases: dark desktop and light phone layouts, period switch, ranking, downloads, prices.
