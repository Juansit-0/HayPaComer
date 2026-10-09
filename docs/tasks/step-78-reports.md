# Step 78: Reports with Template Method and Visitor

Commit and pull request title: `feat(analytics): reports with template method and visitor`

## Goal

A household can download its kitchen numbers as a spreadsheet (CSV) or as a readable report (Markdown) to share or keep. Both formats come from the same metrics and the same walk over the report sections, so adding a format never changes the numbers.

## Scope

- Domain `analytics.report`:
  - Visitor: sealed `ReportSection` (`Summary`, `Foods`, `Members`, `Trend`) with `accept(ReportVisitor)`; `ReportContext` builds the sections from `HouseholdMetrics` with the household name, currency, and member names ("Former member" when unknown).
  - Template Method: `ReportTemplate.render` is final: header, then every section visited in order, then footer; formats only fill in the steps.
  - `CsvReport`: one row per total, money, food, member, and day; cells with commas or quotes are quoted, and cells starting with `=`, `+`, `-`, or `@` are prefixed to block spreadsheet formula injection.
  - `MarkdownReport`: kilograms, waste percentage, money in the household currency, tables per food and member, days with activity; table-breaking characters are cleaned and empty periods say so.
- Application: `ReportFormat` (CSV, MARKDOWN), `ExportedReport`, and `ExportHouseholdReport` (any member; reuses `ViewHouseholdMetrics` and member display names).
- Web: `GET /households/{h}/analytics/report?format=csv|markdown&from=&to=` downloads `haypacomer-<from>-<to>.<ext>` as an attachment (400 for other formats).

## Tests (definition of done)

- `ReportsTest`: CSV order and escaping (including formula injection), Markdown content, empty periods, and a custom visitor walking the same sections.
- `ExportHouseholdReportTest`: both formats with member names and money.
- `AnalyticsIntegrationTest`: CSV download headers and rows, Markdown by default, unknown format 400, stranger 404.
