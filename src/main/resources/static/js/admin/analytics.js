'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var currentCurrency = 'USD';
    var moneyFormatter = createMoneyFormatter(currentCurrency);
    var ACTIVE_STUDENT_SUMMARY = 'Students with learning activity in the selected period.';
    var ACTIVE_STUDENT_UNAVAILABLE = 'Active student definition unavailable.';

    function normalizeCurrency(currency) {
        return currency && /^[A-Z]{3}$/.test(currency) ? currency : 'USD';
    }

    function createMoneyFormatter(currency) {
        var cleanCurrency = normalizeCurrency(currency);
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: cleanCurrency,
            minimumFractionDigits: cleanCurrency === 'VND' ? 0 : 2,
            maximumFractionDigits: cleanCurrency === 'VND' ? 0 : 2
        });
    }

    function setMoneyCurrency(currency, selectors) {
        currentCurrency = normalizeCurrency(currency);
        moneyFormatter = createMoneyFormatter(currentCurrency);
        selectors.forEach(function(selector) {
            document.querySelectorAll(selector).forEach(function(node) {
                node.textContent = currentCurrency;
            });
        });
    }

    function toDateInputValue(date) {
        var year = date.getFullYear();
        var month = String(date.getMonth() + 1).padStart(2, '0');
        var day = String(date.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    }

    function setDefaultDates(form) {
        var to = new Date();
        var from = new Date();
        from.setDate(to.getDate() - 29);
        form.elements.to.value = toDateInputValue(to);
        form.elements.from.value = toDateInputValue(from);
        if (form.elements.groupBy) {
            form.elements.groupBy.value = 'day';
        }
    }

    function appendDates(params, form) {
        ['from', 'to', 'groupBy'].forEach(function(name) {
            if (form.elements[name] && form.elements[name].value) {
                params.set(name, form.elements[name].value);
            }
        });
    }

    function fetchApi(url) {
        return fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
            .then(function(response) {
                return response.json().catch(function() {
                    return { message: 'Unable to read server response.' };
                }).then(function(payload) {
                    if (!response.ok || payload.code >= 400) {
                        throw new Error(payload.message || 'Request failed.');
                    }
                    return payload.data;
                });
            });
    }

    function setState(prefix, state, message) {
        var loading = document.getElementById(prefix + 'Loading');
        var content = document.getElementById(prefix + 'Content');
        var error = document.getElementById(prefix + 'Error');
        if (loading) loading.hidden = state !== 'loading';
        if (content) content.hidden = state !== 'content';
        if (error) {
            error.hidden = state !== 'error';
            if (message) {
                error.querySelector('[data-error-message]').textContent = message;
            }
        }
    }

    function safeNumber(value) {
        var numeric = Number(value);
        return Number.isFinite(numeric) ? numeric : 0;
    }

    function number(value) {
        return safeNumber(value).toLocaleString('en-US');
    }

    function money(value) {
        return moneyFormatter.format(safeNumber(value));
    }

    function setActiveStudentMethod(node, technicalMethod, unavailable) {
        if (!node) return;
        node.textContent = unavailable ? ACTIVE_STUDENT_UNAVAILABLE : ACTIVE_STUDENT_SUMMARY;
        node.setAttribute('data-technical-method', technicalMethod || '');
    }

    function setFormBusy(form, busy) {
        Array.prototype.forEach.call(form.elements, function(control) {
            control.disabled = busy;
        });
    }

    function renderSingleBarChart(container, points, valueKey, labelBuilder) {
        container.innerHTML = '';
        container.setAttribute('role', 'img');
        container.setAttribute('aria-label', 'Revenue bar chart by period. Hover or focus bars for values.');
        if (!hasSeriesData(points, [valueKey])) {
            renderChartState(container, 'empty', 'No revenue data', 'No paid orders were found for this period.');
            return;
        }
        var maxValue = points.reduce(function(max, point) {
            return Math.max(max, safeNumber(point[valueKey]));
        }, 0);
        points.forEach(function(point) {
            var group = document.createElement('div');
            group.className = 'admin-chart-group';
            var barWrap = document.createElement('div');
            barWrap.className = 'admin-bar-single';
            var bar = document.createElement('div');
            bar.className = 'admin-chart-bar';
            bar.style.height = barHeight(point[valueKey], maxValue, container);
            bar.title = labelBuilder(point);
            bar.setAttribute('aria-label', bar.title);
            bar.setAttribute('tabindex', '0');
            var label = document.createElement('div');
            label.className = 'admin-chart-label';
            label.textContent = point.period || 'Period';
            barWrap.appendChild(bar);
            group.appendChild(barWrap);
            group.appendChild(label);
            container.appendChild(group);
        });
    }

    function renderStudentLineChart(container, points) {
        container.innerHTML = '';
        container.setAttribute('role', 'img');
        container.setAttribute('aria-label', 'Student line chart by period. Blue shows new students and cyan shows active students. Focus data points for values.');
        if (!hasSeriesData(points, ['newStudents', 'activeStudents'])) {
            renderChartState(container, 'empty', 'No student activity', 'No new or active students were found for this period.');
            return;
        }

        var maxValue = points.reduce(function(max, point) {
            return Math.max(max, safeNumber(point.newStudents), safeNumber(point.activeStudents));
        }, 0);

        var chart = document.createElement('div');
        chart.className = 'admin-line-canvas';
        chart.style.minWidth = Math.max(680, points.length * 82) + 'px';

        var plot = document.createElement('div');
        plot.className = 'admin-line-plot-area';

        function linePath(valueKey) {
            return points.map(function(point, index) {
                var x = points.length === 1 ? 500 : (index / (points.length - 1)) * 1000;
                var y = 12 + (1 - (safeNumber(point[valueKey]) / maxValue)) * 226;
                return (index === 0 ? 'M ' : 'L ') + x.toFixed(2) + ' ' + y.toFixed(2);
            }).join(' ');
        }

        plot.innerHTML = '<svg class="admin-line-svg" viewBox="0 0 1000 250" preserveAspectRatio="none" aria-hidden="true" focusable="false">' +
            '<path class="admin-line-path admin-line-path-primary" vector-effect="non-scaling-stroke" d="' + linePath('newStudents') + '"></path>' +
            '<path class="admin-line-path admin-line-path-secondary" vector-effect="non-scaling-stroke" d="' + linePath('activeStudents') + '"></path>' +
            '</svg>';

        function addPoints(valueKey, seriesLabel, className) {
            points.forEach(function(point, index) {
                var x = points.length === 1 ? 50 : (index / (points.length - 1)) * 100;
                var y = 4.8 + (1 - (safeNumber(point[valueKey]) / maxValue)) * 90.4;
                var marker = document.createElement('button');
                var description = (point.period || 'Period') + ': ' + number(point[valueKey]) + ' ' + seriesLabel;
                marker.type = 'button';
                marker.className = 'admin-line-point ' + className;
                marker.style.left = x.toFixed(2) + '%';
                marker.style.top = y.toFixed(2) + '%';
                marker.title = description;
                marker.setAttribute('aria-label', description);
                plot.appendChild(marker);
            });
        }

        addPoints('newStudents', 'new students', 'admin-line-point-primary');
        addPoints('activeStudents', 'active students', 'admin-line-point-secondary');

        var labels = document.createElement('div');
        labels.className = 'admin-line-labels';
        labels.style.gridTemplateColumns = 'repeat(' + points.length + ', minmax(0, 1fr))';
        points.forEach(function(point) {
            var label = document.createElement('div');
            label.className = 'admin-line-label';
            label.textContent = point.period || 'Period';
            label.title = point.period || 'Period';
            labels.appendChild(label);
        });

        chart.appendChild(plot);
        chart.appendChild(labels);
        container.appendChild(chart);
    }

    function barHeight(value, maxValue, container) {
        if (!maxValue) {
            return '3px';
        }
        var isTallChart = container && container.classList && container.classList.contains('admin-analytics-chart-tall');
        var availableHeight = isTallChart ? 270 : 210;
        return Math.max(3, Math.round((safeNumber(value) / maxValue) * availableHeight)) + 'px';
    }

    function hasSeriesData(points, valueKeys) {
        return Array.isArray(points) && points.some(function(point) {
            return valueKeys.some(function(key) {
                return safeNumber(point[key]) > 0;
            });
        });
    }

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

    function renderChartState(container, state, title, description) {
        if (!container) return;
        var icon = state === 'loading' ? '<span class="spinner"></span>' : '';
        container.innerHTML = '<div class="empty-state admin-chart-state admin-chart-state-' + state + '">' +
            icon +
            '<div class="empty-state-title">' + escape(title) + '</div>' +
            '<div class="empty-state-desc">' + escape(description || '') + '</div>' +
            '</div>';
    }

    function validateOverview(data) {
        if (!data || typeof data !== 'object') {
            throw new Error('Overview response is invalid.');
        }
        ['totalUsers', 'totalStudents', 'totalTeachers', 'totalCourses', 'totalEnrollments', 'paidOrderCount', 'totalRevenue', 'newStudents', 'activeStudents'].forEach(function(field) {
            if (data[field] == null) {
                throw new Error('Overview response is missing ' + field + '.');
            }
        });
        return data;
    }

    function validateStudentAnalytics(data) {
        if (!data || typeof data !== 'object') {
            throw new Error('Student analytics response is invalid.');
        }
        if (!Array.isArray(data.trend)) {
            throw new Error('Student analytics response is missing trend data.');
        }
        ['newStudents', 'activeStudents'].forEach(function(field) {
            if (data[field] == null) {
                throw new Error('Student analytics response is missing ' + field + '.');
            }
        });
        return data;
    }

    function validateRevenueAnalytics(data) {
        if (!data || typeof data !== 'object') {
            throw new Error('Revenue analytics response is invalid.');
        }
        if (!Array.isArray(data.trend)) {
            throw new Error('Revenue analytics response is missing trend data.');
        }
        ['totalRevenue', 'paidOrderCount'].forEach(function(field) {
            if (data[field] == null) {
                throw new Error('Revenue analytics response is missing ' + field + '.');
            }
        });
        return data;
    }

    function periodText(data) {
        if (!data || !data.from || !data.to) {
            return 'Selected period';
        }
        return data.from + ' to ' + data.to;
    }

    function setDashboardShellState(state, message) {
        var loading = document.getElementById('adminDashboardLoading');
        var content = document.getElementById('adminDashboardContent');
        var error = document.getElementById('adminDashboardError');
        if (loading) loading.hidden = state !== 'loading';
        if (content) content.hidden = state === 'error';
        if (error) {
            error.hidden = state !== 'error';
            if (message) {
                error.querySelector('[data-error-message]').textContent = message;
            }
        }
    }

    function setOverviewState(state, message) {
        var loading = document.getElementById('dashboardOverviewLoading');
        var content = document.getElementById('dashboardOverviewContent');
        var empty = document.getElementById('dashboardOverviewEmpty');
        var error = document.getElementById('dashboardOverviewError');
        if (loading) loading.hidden = state !== 'loading';
        if (content) content.hidden = state !== 'success';
        if (empty) empty.hidden = state !== 'empty';
        if (error) {
            error.hidden = state !== 'error';
            if (message) {
                error.querySelector('[data-error-message]').textContent = message;
            }
        }
    }

    function initDashboard() {
        var form = document.getElementById('adminDashboardFilters');
        if (!form) return;
        var state = { loading: false };

        function load() {
            if (state.loading) return;
            state.loading = true;
            setFormBusy(form, true);
            setDashboardShellState('loading');
            setOverviewState('loading');
            var params = new URLSearchParams();
            appendDates(params, form);
            var studentsChart = document.getElementById('dashboardStudentsChart');
            var revenueChart = document.getElementById('dashboardRevenueChart');
            renderChartState(studentsChart, 'loading', 'Loading student growth', 'Preparing the selected period.');
            renderChartState(revenueChart, 'loading', 'Loading revenue trend', 'Preparing the selected period.');

            Promise.allSettled([
                fetchApi('/api/admin/dashboard/overview?' + params.toString()),
                fetchApi('/api/admin/analytics/students?' + params.toString() + '&groupBy=day'),
                fetchApi('/api/admin/analytics/revenue?' + params.toString() + '&groupBy=day')
            ])
                .then(function(results) {
                    var fulfilledCount = 0;

                    if (results[0].status === 'fulfilled') {
                        try {
                            renderOverview(results[0].value);
                            fulfilledCount += 1;
                        } catch (error) {
                            renderOverviewUnavailable();
                            setOverviewState('error', 'Overview render failed: ' + error.message);
                        }
                    } else {
                        renderOverviewUnavailable();
                        setOverviewState('error', errorMessage(results[0].reason));
                    }

                    if (results[1].status === 'fulfilled') {
                        try {
                            renderDashboardStudentPieChart(studentsChart, validateStudentAnalytics(results[1].value).trend);
                            fulfilledCount += 1;
                        } catch (error) {
                            renderChartState(studentsChart, 'error', 'Student chart unavailable', 'Student chart render failed: ' + error.message);
                        }
                    } else {
                        renderChartState(studentsChart, 'error', 'Student chart unavailable', errorMessage(results[1].reason));
                    }

                    if (results[2].status === 'fulfilled') {
                        try {
                            var revenueData = validateRevenueAnalytics(results[2].value);
                            setMoneyCurrency(revenueData.currency, ['[data-dashboard-revenue-currency]']);
                            renderDashboardRevenuePieChart(revenueChart, revenueData.trend);
                            fulfilledCount += 1;
                        } catch (error) {
                            renderChartState(revenueChart, 'error', 'Revenue chart unavailable', 'Revenue chart render failed: ' + error.message);
                        }
                    } else {
                        renderChartState(revenueChart, 'error', 'Revenue chart unavailable', errorMessage(results[2].reason));
                    }

                    if (fulfilledCount > 0) {
                        setDashboardShellState('success');
                    } else {
                        setDashboardShellState('error', 'Unable to load dashboard widgets.');
                    }
                })
                .finally(function() {
                    state.loading = false;
                    setFormBusy(form, false);
                });
        }

        function renderOverview(data) {
            data = validateOverview(data);
            setMoneyCurrency(data.currency, ['[data-dashboard-currency]']);
            document.querySelectorAll('[data-overview]').forEach(function(node) {
                node.textContent = number(data[node.getAttribute('data-overview')]);
            });
            document.querySelectorAll('[data-overview-money]').forEach(function(node) {
                node.textContent = money(data[node.getAttribute('data-overview-money')]);
            });
            document.querySelector('[data-overview-period]').textContent = periodText(data);
            var activePeriod = document.querySelector('[data-overview-active-period]');
            if (activePeriod) {
                activePeriod.textContent = periodText(data);
            }
            var activeMethod = document.querySelector('[data-overview-active-method]');
            if (activeMethod) {
                setActiveStudentMethod(activeMethod, data.activeStudentMethod);
            }
            if (isOverviewEmpty(data)) {
                setOverviewState('empty');
            } else {
                setOverviewState('success');
            }
        }

        function renderOverviewUnavailable() {
            setMoneyCurrency('USD', ['[data-dashboard-currency]']);
            document.querySelectorAll('[data-overview], [data-overview-money]').forEach(function(node) {
                node.textContent = '--';
            });
            document.querySelector('[data-overview-period]').textContent = 'Selected period unavailable';
            var activePeriod = document.querySelector('[data-overview-active-period]');
            if (activePeriod) {
                activePeriod.textContent = 'Selected period unavailable';
            }
            var activeMethod = document.querySelector('[data-overview-active-method]');
            if (activeMethod) {
                setActiveStudentMethod(activeMethod, '', true);
            }
            setOverviewState('error');
        }

        function isOverviewEmpty(data) {
            return ['totalUsers', 'totalStudents', 'totalTeachers', 'totalCourses', 'totalEnrollments', 'paidOrderCount', 'newStudents', 'activeStudents']
                .every(function(field) {
                    return safeNumber(data[field]) === 0;
                }) && safeNumber(data.totalRevenue) === 0;
        }

        function errorMessage(reason) {
            return reason && reason.message ? reason.message : 'Unable to load this widget.';
        }

        form.addEventListener('submit', function(event) {
            event.preventDefault();
            load();
        });
        document.querySelector('[data-reset-dashboard]').addEventListener('click', function() {
            setDefaultDates(form);
            load();
        });
        setDefaultDates(form);
        load();
    }

    function initStudents() {
        var form = document.getElementById('adminStudentsAnalyticsFilters');
        if (!form) return;
        var state = { loading: false };

        function load() {
            if (state.loading) return;
            state.loading = true;
            setFormBusy(form, true);
            setState('adminStudents', 'loading');
            clearStudentsView();
            var params = new URLSearchParams();
            appendDates(params, form);
            fetchApi('/api/admin/analytics/students?' + params.toString())
                .then(function(data) {
                    data = validateStudentAnalytics(data);
                    renderStudentKpis(data);
                    renderStudentMetadata(data);
                    document.getElementById('adminStudentsEmpty').hidden = safeNumber(data.newStudents) > 0 || safeNumber(data.activeStudents) > 0;
                    renderStudentLineChart(document.getElementById('adminStudentsChart'), data.trend);
                    renderStudentsTable(data.trend);
                    setState('adminStudents', 'content');
                })
                .catch(function(error) {
                    setState('adminStudents', 'error', error.message);
                })
                .finally(function() {
                    state.loading = false;
                    setFormBusy(form, false);
                });
        }

        form.addEventListener('submit', function(event) {
            event.preventDefault();
            load();
        });
        document.querySelector('[data-reset-students]').addEventListener('click', function() {
            setDefaultDates(form);
            load();
        });
        setDefaultDates(form);
        load();
    }

    function clearStudentsView() {
        document.querySelector('[data-students-new]').textContent = '0';
        document.querySelector('[data-students-active]').textContent = '0';
        setActiveStudentMethod(document.querySelector('[data-students-method]'), '');
        document.querySelector('[data-students-period]').textContent = 'Selected period';
        document.querySelector('[data-students-chart-period]').textContent = '';
        document.querySelector('[data-students-timezone]').textContent = '';
        document.getElementById('adminStudentsEmpty').hidden = true;
        renderChartState(document.getElementById('adminStudentsChart'), 'loading', 'Loading student trend', 'Preparing the selected period.');
        document.getElementById('adminStudentsTableBody').innerHTML = '';
    }

    function renderStudentKpis(data) {
        document.querySelector('[data-students-new]').textContent = number(data.newStudents);
        document.querySelector('[data-students-active]').textContent = number(data.activeStudents);
    }

    function renderStudentMetadata(data) {
        setActiveStudentMethod(document.querySelector('[data-students-method]'), data.activeStudentMethod);
        document.querySelector('[data-students-period]').textContent = periodText(data);
        document.querySelector('[data-students-chart-period]').textContent = periodText(data);
        document.querySelector('[data-students-timezone]').textContent = data.timeZone ? 'Time zone: ' + data.timeZone : '';
    }

    function renderStudentsTable(points) {
        if (!points.length) {
            document.getElementById('adminStudentsTableBody').innerHTML = '<tr><td colspan="3">No student trend rows for the selected period.</td></tr>';
            return;
        }
        document.getElementById('adminStudentsTableBody').innerHTML = points.map(function(point) {
            return '<tr><td>' + escape(point.period) + '</td><td>' + number(point.newStudents) + '</td><td>' + number(point.activeStudents) + '</td></tr>';
        }).join('');
    }

    function initRevenue() {
        var form = document.getElementById('adminRevenueAnalyticsFilters');
        if (!form) return;
        var state = { loading: false };

        function load() {
            if (state.loading) return;
            state.loading = true;
            setFormBusy(form, true);
            setState('adminRevenue', 'loading');
            clearRevenueView();
            var params = new URLSearchParams();
            appendDates(params, form);
            fetchApi('/api/admin/analytics/revenue?' + params.toString())
                .then(function(data) {
                    data = validateRevenueAnalytics(data);
                    renderRevenueKpis(data);
                    renderRevenueMetadata(data);
                    document.getElementById('adminRevenueEmpty').hidden = safeNumber(data.totalRevenue) > 0 || safeNumber(data.paidOrderCount) > 0;
                    renderSingleBarChart(document.getElementById('adminRevenueChart'), data.trend, 'revenue', function(point) {
                        return point.period + ': ' + money(point.revenue) + ', ' + number(point.paidOrderCount) + ' paid orders';
                    });
                    renderRevenueTable(data.trend);
                    setState('adminRevenue', 'content');
                })
                .catch(function(error) {
                    setState('adminRevenue', 'error', error.message);
                })
                .finally(function() {
                    state.loading = false;
                    setFormBusy(form, false);
                });
        }

        form.addEventListener('submit', function(event) {
            event.preventDefault();
            load();
        });
        document.querySelector('[data-reset-revenue]').addEventListener('click', function() {
            setDefaultDates(form);
            load();
        });
        setDefaultDates(form);
        load();
    }

    function clearRevenueView() {
        setMoneyCurrency('USD', ['[data-revenue-currency]', '[data-revenue-chart-currency]']);
        document.querySelector('[data-revenue-total]').textContent = money(0);
        document.querySelector('[data-revenue-orders]').textContent = '0';
        document.querySelector('[data-revenue-period]').textContent = '';
        document.querySelector('[data-revenue-chart-period]').textContent = 'Paid orders only';
        document.querySelector('[data-revenue-timezone]').textContent = '';
        document.querySelector('[data-revenue-method]').textContent = 'Paid order items by order creation date';
        document.getElementById('adminRevenueEmpty').hidden = true;
        renderChartState(document.getElementById('adminRevenueChart'), 'loading', 'Loading revenue trend', 'Preparing the selected period.');
        document.getElementById('adminRevenueTableBody').innerHTML = '';
    }

    function renderRevenueKpis(data) {
        setMoneyCurrency(data.currency, ['[data-revenue-currency]', '[data-revenue-chart-currency]']);
        document.querySelector('[data-revenue-total]').textContent = money(data.totalRevenue);
        document.querySelector('[data-revenue-orders]').textContent = number(data.paidOrderCount);
    }

    function renderRevenueMetadata(data) {
        document.querySelector('[data-revenue-period]').textContent = periodText(data);
        document.querySelector('[data-revenue-chart-period]').textContent = periodText(data) + ' by ' + String(data.groupBy || 'day').toUpperCase();
        document.querySelector('[data-revenue-timezone]').textContent = data.timeZone ? 'Time zone: ' + data.timeZone : '';
        document.querySelector('[data-revenue-method]').textContent = data.revenueRecognitionMethod || 'Paid order items by order creation date';
    }

    function renderRevenueTable(points) {
        if (!points.length) {
            document.getElementById('adminRevenueTableBody').innerHTML = '<tr><td colspan="3">No revenue rows for the selected period.</td></tr>';
            return;
        }
        document.getElementById('adminRevenueTableBody').innerHTML = points.map(function(point) {
            return '<tr><td>' + escape(point.period) + '</td><td>' + money(point.revenue) + '</td><td>' + number(point.paidOrderCount) + '</td></tr>';
        }).join('');
    }

    function escape(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    initDashboard();
    initStudents();
    initRevenue();
});
