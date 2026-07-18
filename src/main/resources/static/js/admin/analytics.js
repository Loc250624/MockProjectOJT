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
            bar.style.height = barHeight(point[valueKey], maxValue);
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

    function renderStudentBarChart(container, points) {
        container.innerHTML = '';
        container.setAttribute('role', 'img');
        container.setAttribute('aria-label', 'Student bar chart by period. Blue bars show new students and cyan bars show active students.');
        if (!hasSeriesData(points, ['newStudents', 'activeStudents'])) {
            renderChartState(container, 'empty', 'No student activity', 'No new or active students were found for this period.');
            return;
        }
        var maxValue = points.reduce(function(max, point) {
            return Math.max(max, safeNumber(point.newStudents), safeNumber(point.activeStudents));
        }, 0);
        points.forEach(function(point) {
            var group = document.createElement('div');
            group.className = 'admin-chart-group';
            var pair = document.createElement('div');
            pair.className = 'admin-bar-pair';
            var newBar = document.createElement('div');
            newBar.className = 'admin-chart-bar';
            newBar.style.height = barHeight(point.newStudents, maxValue);
            newBar.title = point.period + ': ' + number(point.newStudents) + ' new students';
            newBar.setAttribute('aria-label', newBar.title);
            newBar.setAttribute('tabindex', '0');
            var activeBar = document.createElement('div');
            activeBar.className = 'admin-chart-bar secondary';
            activeBar.style.height = barHeight(point.activeStudents, maxValue);
            activeBar.title = point.period + ': ' + number(point.activeStudents) + ' active students';
            activeBar.setAttribute('aria-label', activeBar.title);
            activeBar.setAttribute('tabindex', '0');
            var label = document.createElement('div');
            label.className = 'admin-chart-label';
            label.textContent = point.period || 'Period';
            pair.appendChild(newBar);
            pair.appendChild(activeBar);
            group.appendChild(pair);
            group.appendChild(label);
            container.appendChild(group);
        });
    }

    function barHeight(value, maxValue) {
        if (!maxValue) {
            return '3px';
        }
        return Math.max(3, Math.round((safeNumber(value) / maxValue) * 210)) + 'px';
    }

    function hasSeriesData(points, valueKeys) {
        return Array.isArray(points) && points.some(function(point) {
            return valueKeys.some(function(key) {
                return safeNumber(point[key]) > 0;
            });
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
                            renderStudentBarChart(studentsChart, validateStudentAnalytics(results[1].value).trend);
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
                            renderSingleBarChart(revenueChart, revenueData.trend, 'revenue', function(point) {
                                return point.period + ': ' + money(point.revenue) + ', ' + number(point.paidOrderCount) + ' paid orders';
                            });
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
                    renderStudentBarChart(document.getElementById('adminStudentsChart'), data.trend);
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
