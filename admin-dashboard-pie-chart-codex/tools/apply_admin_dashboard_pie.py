#!/usr/bin/env python3
"""
Apply the Admin Dashboard pie/donut chart change to MockProjectOJT.

The script intentionally uses exact, guarded replacements. It aborts rather
than guessing when the repository has diverged from the expected source.
"""

from __future__ import annotations

import argparse
from pathlib import Path
import sys


DASHBOARD_PATH = Path("src/main/resources/templates/admin/dashboard.html")
ANALYTICS_JS_PATH = Path("src/main/resources/static/js/admin/analytics.js")
ADMIN_CSS_PATH = Path("src/main/resources/static/css/admin/admin.css")


PIE_JS = r"""
    var DASHBOARD_PIE_COLORS = ['#075ed1', '#14b8a6', '#8b5cf6', '#f59e0b', '#ef4444', '#0ea5e9'];

    function sumTrend(points, valueKey) {
        if (!Array.isArray(points)) return 0;
        return points.reduce(function(total, point) {
            return total + safeNumber(point[valueKey]);
        }, 0);
    }

    function renderDashboardPieChart(container, slices, options) {
        if (!container) return;
        options = options || {};
        var cleanSlices = (Array.isArray(slices) ? slices : []).filter(function(slice) {
            return slice && safeNumber(slice.value) > 0;
        }).map(function(slice, index) {
            return {
                label: slice.label || 'Value',
                value: safeNumber(slice.value),
                color: slice.color || DASHBOARD_PIE_COLORS[index % DASHBOARD_PIE_COLORS.length]
            };
        });

        if (!cleanSlices.length) {
            renderChartState(
                container,
                'empty',
                options.emptyTitle || 'No chart data',
                options.emptyDescription || 'No data was found for this period.'
            );
            return;
        }

        var total = cleanSlices.reduce(function(sum, slice) {
            return sum + slice.value;
        }, 0);
        var formatter = typeof options.valueFormatter === 'function' ? options.valueFormatter : number;
        var cursor = 0;
        var gradientStops = cleanSlices.map(function(slice) {
            var start = cursor;
            cursor += (slice.value / total) * 100;
            return slice.color + ' ' + start.toFixed(2) + '% ' + cursor.toFixed(2) + '%';
        });

        container.innerHTML = '';
        container.setAttribute('role', 'img');
        container.setAttribute(
            'aria-label',
            options.ariaLabel || cleanSlices.map(function(slice) {
                return slice.label + ': ' + formatter(slice.value);
            }).join('. ')
        );

        var layout = document.createElement('div');
        layout.className = 'admin-pie-layout';

        var visual = document.createElement('div');
        visual.className = 'admin-pie-visual';
        visual.style.background = 'conic-gradient(' + gradientStops.join(', ') + ')';
        visual.setAttribute('aria-hidden', 'true');

        var hole = document.createElement('div');
        hole.className = 'admin-pie-hole';

        var centerValue = document.createElement('strong');
        centerValue.className = 'admin-pie-center-value';
        centerValue.textContent = options.centerValue || formatter(total);

        var centerLabel = document.createElement('span');
        centerLabel.className = 'admin-pie-center-label';
        centerLabel.textContent = options.centerLabel || 'total';

        hole.appendChild(centerValue);
        hole.appendChild(centerLabel);
        visual.appendChild(hole);

        var details = document.createElement('div');
        details.className = 'admin-pie-details';

        var legend = document.createElement('ul');
        legend.className = 'admin-pie-legend';

        cleanSlices.forEach(function(slice) {
            var percentage = total > 0 ? (slice.value / total) * 100 : 0;
            var item = document.createElement('li');
            item.className = 'admin-pie-legend-item';
            item.setAttribute('tabindex', '0');
            item.setAttribute(
                'aria-label',
                slice.label + ': ' + formatter(slice.value) + ', ' + percentage.toFixed(1) + ' percent'
            );

            var swatch = document.createElement('i');
            swatch.className = 'admin-pie-swatch';
            swatch.style.background = slice.color;
            swatch.setAttribute('aria-hidden', 'true');

            var label = document.createElement('span');
            label.className = 'admin-pie-legend-label';
            label.textContent = slice.label;

            var value = document.createElement('strong');
            value.className = 'admin-pie-legend-value';
            value.textContent = formatter(slice.value);

            var percent = document.createElement('span');
            percent.className = 'admin-pie-legend-percent';
            percent.textContent = percentage.toFixed(1) + '%';

            item.appendChild(swatch);
            item.appendChild(label);
            item.appendChild(value);
            item.appendChild(percent);
            legend.appendChild(item);
        });

        details.appendChild(legend);

        if (options.note) {
            var note = document.createElement('p');
            note.className = 'admin-pie-note';
            note.textContent = options.note;
            details.appendChild(note);
        }

        layout.appendChild(visual);
        layout.appendChild(details);
        container.appendChild(layout);
    }

    function renderDashboardStudentPieChart(container, points) {
        var newStudents = sumTrend(points, 'newStudents');
        var activeStudents = sumTrend(points, 'activeStudents');

        renderDashboardPieChart(container, [
            { label: 'New students', value: newStudents, color: DASHBOARD_PIE_COLORS[0] },
            { label: 'Active students', value: activeStudents, color: DASHBOARD_PIE_COLORS[1] }
        ], {
            emptyTitle: 'No student activity',
            emptyDescription: 'No new or active students were found for this period.',
            centerValue: number(newStudents + activeStudents),
            centerLabel: 'reported counts',
            valueFormatter: number,
            note: 'New and active student groups can overlap.',
            ariaLabel: 'Student breakdown pie chart. New students: ' + number(newStudents) +
                '. Active students: ' + number(activeStudents) +
                '. New and active groups can overlap.'
        });
    }

    function renderDashboardRevenuePieChart(container, points) {
        var slices = (Array.isArray(points) ? points : []).map(function(point) {
            return {
                label: point.period || 'Period',
                value: safeNumber(point.revenue)
            };
        }).filter(function(slice) {
            return slice.value > 0;
        }).sort(function(left, right) {
            return right.value - left.value;
        });

        var maxNamedSlices = 5;
        if (slices.length > maxNamedSlices) {
            var remaining = slices.slice(maxNamedSlices);
            slices = slices.slice(0, maxNamedSlices);
            slices.push({
                label: 'Other periods',
                value: remaining.reduce(function(total, slice) {
                    return total + slice.value;
                }, 0)
            });
        }

        slices = slices.map(function(slice, index) {
            return {
                label: slice.label,
                value: slice.value,
                color: DASHBOARD_PIE_COLORS[index % DASHBOARD_PIE_COLORS.length]
            };
        });

        var totalRevenue = slices.reduce(function(total, slice) {
            return total + slice.value;
        }, 0);

        renderDashboardPieChart(container, slices, {
            emptyTitle: 'No revenue data',
            emptyDescription: 'No paid orders were found for this period.',
            centerValue: money(totalRevenue),
            centerLabel: 'total revenue',
            valueFormatter: money,
            note: 'The top five periods are shown; remaining periods are combined as Other periods.',
            ariaLabel: 'Revenue breakdown pie chart. ' + slices.map(function(slice) {
                return slice.label + ': ' + money(slice.value);
            }).join('. ')
        });
    }

"""

PIE_CSS = r"""
/* Admin dashboard pie/donut charts */
.admin-analytics-chart.admin-dashboard-pie-chart {
  display: block;
  min-height: 280px;
  padding: 0.75rem 0 0;
  overflow: visible;
  background: none;
}

.admin-dashboard-pie-chart .admin-chart-state {
  min-height: 250px;
}

.admin-pie-layout {
  display: grid;
  grid-template-columns: minmax(160px, 210px) minmax(0, 1fr);
  align-items: center;
  gap: 1.25rem;
  min-height: 250px;
}

.admin-pie-visual {
  position: relative;
  width: min(100%, 210px);
  aspect-ratio: 1;
  margin: 0 auto;
  border-radius: 50%;
  box-shadow:
    inset 0 0 0 1px rgba(15, 23, 42, 0.08),
    0 12px 30px rgba(15, 23, 42, 0.08);
}

.admin-pie-hole {
  position: absolute;
  inset: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
  padding: 0.5rem;
  border-radius: 50%;
  background: white;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.06);
  text-align: center;
}

.admin-pie-center-value {
  max-width: 100%;
  color: var(--lumina-gray-900);
  font-size: clamp(0.78rem, 2vw, 1.15rem);
  line-height: 1.15;
  overflow-wrap: anywhere;
}

.admin-pie-center-label {
  color: var(--lumina-gray-500);
  font-size: 0.68rem;
  font-weight: 700;
  line-height: 1.2;
  text-transform: uppercase;
}

.admin-pie-details {
  min-width: 0;
}

.admin-pie-legend {
  display: grid;
  gap: 0.55rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.admin-pie-legend-item {
  display: grid;
  grid-template-columns: 0.75rem minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  padding: 0.55rem 0.6rem;
  border: 1px solid var(--lumina-gray-200);
  border-radius: 8px;
  background: white;
}

.admin-pie-legend-item:focus-visible {
  outline: 3px solid var(--lumina-blue-pale);
  outline-offset: 2px;
  border-color: var(--lumina-blue);
}

.admin-pie-swatch {
  width: 0.7rem;
  height: 0.7rem;
  border-radius: 999px;
}

.admin-pie-legend-label {
  min-width: 0;
  color: var(--lumina-gray-700);
  font-size: 0.76rem;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-pie-legend-value {
  color: var(--lumina-gray-900);
  font-size: 0.76rem;
  overflow-wrap: anywhere;
  text-align: right;
}

.admin-pie-legend-percent {
  min-width: 3.4rem;
  color: var(--lumina-gray-500);
  font-size: 0.72rem;
  font-weight: 700;
  text-align: right;
}

.admin-pie-note {
  margin: 0.7rem 0 0;
  color: var(--lumina-gray-500);
  font-size: 0.72rem;
  line-height: 1.45;
}

@media (max-width: 900px) {
  .admin-pie-layout {
    grid-template-columns: 1fr;
  }

  .admin-pie-visual {
    width: min(190px, 70vw);
  }
}

@media (max-width: 480px) {
  .admin-analytics-chart.admin-dashboard-pie-chart {
    min-height: 250px;
  }

  .admin-pie-layout {
    gap: 1rem;
    min-height: 230px;
  }

  .admin-pie-visual {
    width: min(170px, 68vw);
  }

  .admin-pie-legend-item {
    grid-template-columns: 0.75rem minmax(0, 1fr) auto;
  }

  .admin-pie-legend-percent {
    grid-column: 2 / -1;
    min-width: 0;
    text-align: left;
  }
}

"""


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def build_changes(root: Path) -> dict[Path, str]:
    dashboard_file = root / DASHBOARD_PATH
    analytics_file = root / ANALYTICS_JS_PATH
    css_file = root / ADMIN_CSS_PATH

    for path in (dashboard_file, analytics_file, css_file):
        if not path.is_file():
            raise FileNotFoundError(f"Required file not found: {path}")

    dashboard = dashboard_file.read_text(encoding="utf-8")
    analytics = analytics_file.read_text(encoding="utf-8")
    css = css_file.read_text(encoding="utf-8")

    dashboard = replace_once(
        dashboard,
        '<h2 class="card-title">Student Growth</h2>\n'
        '                                <p class="card-subtitle">New and active students in the selected period.</p>',
        '<h2 class="card-title">Student Breakdown</h2>\n'
        '                                <p class="card-subtitle">New and active student counts in the selected period.</p>',
        "student card heading",
    )
    dashboard = replace_once(
        dashboard,
        '<span class="admin-chart-axis">X-axis: period</span>\n'
        '                        </div>\n'
        '                        <div id="dashboardStudentsChart" class="admin-analytics-chart" aria-live="polite"></div>',
        '<span class="admin-chart-axis">Pie chart: selected-period totals</span>\n'
        '                        </div>\n'
        '                        <div id="dashboardStudentsChart" class="admin-analytics-chart admin-dashboard-pie-chart" aria-live="polite"></div>',
        "student chart metadata",
    )
    dashboard = replace_once(
        dashboard,
        '<h2 class="card-title">Revenue Trend</h2>\n'
        '                                <p class="card-subtitle">Paid orders only.</p>',
        '<h2 class="card-title">Revenue Breakdown</h2>\n'
        '                                <p class="card-subtitle">Revenue contribution by period from paid orders.</p>',
        "revenue card heading",
    )
    dashboard = replace_once(
        dashboard,
        '<span><i class="admin-chart-key admin-chart-key-primary"></i>Revenue, <span data-dashboard-revenue-currency>USD</span></span>\n'
        '                            <span class="admin-chart-axis">X-axis: period</span>\n'
        '                        </div>\n'
        '                        <div id="dashboardRevenueChart" class="admin-analytics-chart" aria-live="polite"></div>',
        '<span>Revenue contribution, <span data-dashboard-revenue-currency>USD</span></span>\n'
        '                            <span class="admin-chart-axis">Top five periods + Other</span>\n'
        '                        </div>\n'
        '                        <div id="dashboardRevenueChart" class="admin-analytics-chart admin-dashboard-pie-chart" aria-live="polite"></div>',
        "revenue chart metadata",
    )

    marker = "    function renderChartState(container, state, title, description) {\n"
    if "function renderDashboardPieChart(" in analytics:
        raise RuntimeError("analytics.js already appears to contain the dashboard pie renderer")
    analytics = replace_once(analytics, marker, PIE_JS + marker, "pie renderer insertion")

    analytics = replace_once(
        analytics,
        "                            renderStudentBarChart(studentsChart, validateStudentAnalytics(results[1].value).trend);",
        "                            renderDashboardStudentPieChart(studentsChart, validateStudentAnalytics(results[1].value).trend);",
        "dashboard student renderer call",
    )
    analytics = replace_once(
        analytics,
        "                            renderSingleBarChart(revenueChart, revenueData.trend, 'revenue', function(point) {\n"
        "                                return point.period + ': ' + money(point.revenue) + ', ' + number(point.paidOrderCount) + ' paid orders';\n"
        "                            });",
        "                            renderDashboardRevenuePieChart(revenueChart, revenueData.trend);",
        "dashboard revenue renderer call",
    )

    css_marker = ".admin-analytics-table-card {\n"
    if ".admin-dashboard-pie-chart" in css:
        raise RuntimeError("admin.css already appears to contain dashboard pie styles")
    css = replace_once(css, css_marker, PIE_CSS + "\n" + css_marker, "pie CSS insertion")

    return {
        dashboard_file: dashboard,
        analytics_file: analytics,
        css_file: css,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".", help="Repository root")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="Validate source patterns without writing")
    mode.add_argument("--apply", action="store_true", help="Apply the guarded replacements")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    try:
        changes = build_changes(root)
    except (OSError, RuntimeError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    if args.check:
        print("Source patterns verified. The pie-chart change can be applied safely.")
        for path in changes:
            print(f"  - {path.relative_to(root)}")
        return 0

    for path, content in changes.items():
        path.write_text(content, encoding="utf-8", newline="\n")
        print(f"Updated {path.relative_to(root)}")

    print("Done. Review git diff, run git diff --check, then run the test suite.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
