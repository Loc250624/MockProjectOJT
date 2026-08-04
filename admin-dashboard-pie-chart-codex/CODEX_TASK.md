# Codex Task: Replace Admin Dashboard Bar Charts with Pie/Donut Charts

## Repository target

- Project: `Loc250624/MockProjectOJT`
- Scope is limited to the **Admin Dashboard** at `/admin/dashboard`.
- Current relevant files:
  - `src/main/resources/templates/admin/dashboard.html`
  - `src/main/resources/static/js/admin/analytics.js`
  - `src/main/resources/static/css/admin/admin.css`

## Objective

Replace the two bar-chart visualizations shown on the Admin Dashboard with responsive pie/donut charts:

1. **Student Growth** becomes **Student Breakdown**
   - Two slices: `New students` and `Active students`.
   - Use totals aggregated from the already-returned trend data.
   - Keep a visible note that new and active groups may overlap.

2. **Revenue Trend** becomes **Revenue Breakdown**
   - Slices represent revenue contribution by period.
   - To avoid an unreadable 30-slice chart, show the top five revenue periods and combine the remainder into `Other periods`.
   - Continue using the API-provided currency and existing money formatter.

## Important scope limits

- Do **not** change API endpoints, controllers, DTOs, repositories, database queries, or date filters.
- Do **not** add Chart.js, ApexCharts, D3, CDN scripts, or npm dependencies.
- Do **not** change the detailed analytics pages:
  - `/admin/analytics`
  - `/admin/analytics/revenue`
- Their existing bar charts must remain unchanged.
- Preserve loading, empty, error, keyboard-focus, and screen-reader behavior.
- Do not modify the sidebar, top bar, KPI cards, footer, AI chatbot, or unrelated pages.

## Implementation approach

Use native DOM APIs and CSS `conic-gradient`:

- Add dashboard-only renderers in `analytics.js`.
- Keep `renderStudentBarChart()` and `renderSingleBarChart()` intact for detailed reports.
- Change only the two renderer calls inside `initDashboard()`.
- Add dashboard-only chart classes and responsive styles in `admin.css`.
- Update dashboard labels so the UI no longer refers to an X-axis or a trend chart.

## Files supplied in this kit

- `patches/0001-admin-dashboard-pie-charts.patch`
  - Reviewable unified diff for the three project files.
- `tools/apply_admin_dashboard_pie.py`
  - Guarded application script that checks the expected source patterns before writing.
- `reference/admin-dashboard-before.png`
  - Screenshot of the current Admin Dashboard.

## Suggested Codex workflow

```bash
git checkout -b feature/admin-dashboard-pie-chart

python tools/apply_admin_dashboard_pie.py --root . --check
python tools/apply_admin_dashboard_pie.py --root . --apply

git diff --check
./mvnw test
```

Codex may apply the unified patch instead of using the Python helper:

```bash
git apply --check patches/0001-admin-dashboard-pie-charts.patch
git apply patches/0001-admin-dashboard-pie-charts.patch
```

## Acceptance criteria

- `/admin/dashboard` renders two donut charts instead of bars.
- Student chart displays New students and Active students.
- Revenue chart displays top five revenue periods plus Other periods when needed.
- Apply and Reset date filters re-render the charts correctly.
- Currency is still taken from the revenue API response.
- Zero-value data still displays the existing empty-state UI.
- API failures still display the existing error-state UI.
- Charts fit without horizontal scrolling at 360 px, 768 px, and desktop widths.
- Keyboard focus is visible on legend rows.
- Screen readers receive meaningful chart summaries.
- Detailed student and revenue analytics pages retain their existing bar charts.
- No unrelated file is changed.
