'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var form = document.getElementById('teacherAnalyticsFilters');
    if (!form) {
        return;
    }

    var currentCurrency = 'USD';
    var formatter = createMoneyFormatter(currentCurrency);
    var state = { loading: false };

    function createMoneyFormatter(currency) {
        var cleanCurrency = normalizeCurrency(currency);
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: cleanCurrency,
            minimumFractionDigits: cleanCurrency === 'VND' ? 0 : 2,
            maximumFractionDigits: cleanCurrency === 'VND' ? 0 : 2
        });
    }

    function normalizeCurrency(currency) {
        return currency && /^[A-Z]{3}$/.test(currency) ? currency : 'USD';
    }

    function setCurrency(currency) {
        currentCurrency = normalizeCurrency(currency);
        formatter = createMoneyFormatter(currentCurrency);
        document.querySelectorAll('[data-teacher-currency]').forEach(function(node) {
            node.textContent = currentCurrency;
        });
    }

    function toDateInputValue(date) {
        var year = date.getFullYear();
        var month = String(date.getMonth() + 1).padStart(2, '0');
        var day = String(date.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    }

    function setDefaultDates() {
        var to = new Date();
        var from = new Date();
        from.setDate(to.getDate() - 29);
        form.elements.to.value = toDateInputValue(to);
        form.elements.from.value = toDateInputValue(from);
        form.elements.groupBy.value = 'day';
        form.elements.courseId.value = '';
    }

    function buildQuery(includeGroupBy) {
        var params = new URLSearchParams();
        ['from', 'to', 'courseId'].forEach(function(name) {
            var value = form.elements[name].value;
            if (value) {
                params.set(name, value);
            }
        });
        if (includeGroupBy) {
            params.set('groupBy', form.elements.groupBy.value || 'day');
        }
        return params.toString();
    }

    function element(id) {
        return document.getElementById(id);
    }

    function hide(id) {
        var node = element(id);
        if (node) {
            node.hidden = true;
        }
    }

    function show(id) {
        var node = element(id);
        if (node) {
            node.hidden = false;
        }
    }

    function setErrorMessage(id, message) {
        var node = element(id);
        var target = node ? node.querySelector('[data-error-message]') : null;
        if (target) {
            target.textContent = message || 'Please try again.';
        }
    }

    function setPageLoading() {
        show('analyticsLoading');
        hide('analyticsError');
        hide('analyticsContent');
    }

    function setPageReady() {
        hide('analyticsLoading');
        hide('analyticsError');
        show('analyticsContent');
    }

    function setPageError(message) {
        hide('analyticsLoading');
        hide('analyticsContent');
        setErrorMessage('analyticsError', message);
        show('analyticsError');
    }

    function setSummaryState(nextState, message) {
        hide('analyticsSummaryLoading');
        hide('analyticsSummaryError');
        hide('analyticsSummaryEmpty');
        hide('analyticsSummaryContent');
        if (nextState === 'loading') {
            show('analyticsSummaryLoading');
        } else if (nextState === 'empty') {
            show('analyticsSummaryEmpty');
        } else if (nextState === 'error') {
            setErrorMessage('analyticsSummaryError', message);
            show('analyticsSummaryError');
        } else if (nextState === 'success') {
            show('analyticsSummaryContent');
        }
    }

    function setCoursesState(nextState, message) {
        hide('analyticsCoursesLoading');
        hide('analyticsCoursesError');
        hide('analyticsCoursesEmpty');
        hide('analyticsCoursesContent');
        if (nextState === 'loading') {
            show('analyticsCoursesLoading');
        } else if (nextState === 'empty') {
            show('analyticsCoursesEmpty');
        } else if (nextState === 'error') {
            setErrorMessage('analyticsCoursesError', message);
            show('analyticsCoursesError');
        } else if (nextState === 'success') {
            show('analyticsCoursesContent');
        }
    }

    function asMoney(value) {
        return formatter.format(safeNumber(value));
    }

    function asNumber(value) {
        return safeNumber(value).toLocaleString('en-US');
    }

    function safeNumber(value) {
        var numeric = Number(value);
        return Number.isFinite(numeric) ? numeric : 0;
    }

    function setFormBusy(isBusy) {
        Array.prototype.forEach.call(form.elements, function(control) {
            control.disabled = isBusy;
        });
    }

    function fetchJson(url, label) {
        return fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
            .then(function(response) {
                return response.json().catch(function() {
                    throw new Error(label + ' returned an unreadable response.');
                }).then(function(payload) {
                    if (!response.ok || payload.code >= 400) {
                        throw new Error(payload.message || label + ' request failed.');
                    }
                    if (!payload || typeof payload !== 'object' || payload.data == null) {
                        throw new Error(label + ' response is missing data.');
                    }
                    return payload.data;
                });
            });
    }

    function validateSummary(summary) {
        if (!summary || typeof summary !== 'object') {
            throw new Error('Revenue summary response is invalid.');
        }
        if (!Array.isArray(summary.trend)) {
            throw new Error('Revenue summary response is missing trend data.');
        }
        ['totalRevenue', 'paidOrderCount', 'paidStudentCount', 'enrollmentCount', 'studentCount'].forEach(function(field) {
            if (summary[field] == null) {
                throw new Error('Revenue summary response is missing ' + field + '.');
            }
        });
        return summary;
    }

    function validateCourses(payload) {
        if (!payload || typeof payload !== 'object' || !Array.isArray(payload.courses)) {
            throw new Error('Revenue by course response is invalid.');
        }
        return payload.courses;
    }

    function clearSummary() {
        document.querySelector('[data-total-revenue]').textContent = asMoney(0);
        document.querySelector('[data-paid-orders]').textContent = '0';
        document.querySelector('[data-paid-students]').textContent = '0';
        document.querySelector('[data-enrollments]').textContent = '0';
        document.querySelector('[data-students]').textContent = '0';
        document.querySelector('[data-best-course]').textContent = 'None yet';
        document.querySelector('[data-period-label]').textContent = '';
        document.querySelectorAll('[data-kpi-period]').forEach(function(node) {
            var unit = node.getAttribute('data-kpi-unit');
            node.textContent = unit ? kpiUnitLabel(unit) + ', selected period' : 'selected period';
        });
        var chart = element('revenueTrendChart');
        var snapshot = element('courseSnapshot');
        if (chart) chart.innerHTML = '';
        if (snapshot) snapshot.innerHTML = '';
    }

    function clearCourses() {
        var body = element('revenueByCourseBody');
        var cards = element('revenueByCourseCards');
        if (body) body.innerHTML = '';
        if (cards) cards.innerHTML = '';
        document.querySelector('[data-course-count]').textContent = '0 courses';
    }

    function renderSummary(summary) {
        summary = validateSummary(summary);
        setCurrency(summary.currency);
        var selectedPeriod = (summary.from && summary.to) ? summary.from + ' to ' + summary.to : 'Selected period';
        document.querySelector('[data-total-revenue]').textContent = asMoney(summary.totalRevenue);
        document.querySelector('[data-paid-orders]').textContent = asNumber(summary.paidOrderCount);
        document.querySelector('[data-paid-students]').textContent = asNumber(summary.paidStudentCount);
        document.querySelector('[data-enrollments]').textContent = asNumber(summary.enrollmentCount);
        document.querySelector('[data-students]').textContent = asNumber(summary.studentCount);
        document.querySelector('[data-best-course]').textContent = summary.bestSellingCourse && summary.bestSellingCourse.courseTitle
            ? summary.bestSellingCourse.courseTitle
            : 'None yet';
        document.querySelector('[data-period-label]').textContent = selectedPeriod;
        document.querySelectorAll('[data-kpi-period]').forEach(function(node) {
            var unit = node.getAttribute('data-kpi-unit');
            node.textContent = unit ? kpiUnitLabel(unit) + ', ' + selectedPeriod : selectedPeriod;
        });
        renderTrend(summary.trend);
        if (safeNumber(summary.totalRevenue) > 0 || safeNumber(summary.paidOrderCount) > 0) {
            setSummaryState('success');
        } else {
            setSummaryState('empty');
        }
    }

    function kpiUnitLabel(unit) {
        return unit === 'currency' ? currentCurrency : unit;
    }

    function renderTrend(points) {
        var chart = element('revenueTrendChart');
        if (!chart) {
            throw new Error('Revenue trend chart element is missing.');
        }
        chart.innerHTML = '';
        chart.setAttribute('role', 'img');
        chart.setAttribute('aria-label', 'Revenue bar chart by period. Hover or focus bars for revenue and paid order values.');
        if (!Array.isArray(points) || !points.length) {
            chart.innerHTML = '<div class="teacher-state report-inline-state"><strong>No trend data.</strong><span>No paid orders were found for this period.</span></div>';
            return;
        }
        var maxRevenue = points.reduce(function(max, point) {
            return Math.max(max, safeNumber(point.revenue));
        }, 0);
        points.forEach(function(point) {
            var wrap = document.createElement('div');
            wrap.className = 'analytics-bar-wrap';
            var bar = document.createElement('div');
            bar.className = 'analytics-bar';
            var height = maxRevenue === 0 ? 3 : Math.max(3, Math.round((safeNumber(point.revenue) / maxRevenue) * 220));
            bar.style.height = height + 'px';
            bar.title = (point.period || 'Period') + ': ' + asMoney(point.revenue) + ' from ' + asNumber(point.paidOrderCount) + ' paid orders';
            bar.setAttribute('aria-label', bar.title);
            bar.setAttribute('tabindex', '0');
            var label = document.createElement('div');
            label.className = 'analytics-bar-label';
            label.textContent = point.period || 'Period';
            wrap.appendChild(bar);
            wrap.appendChild(label);
            chart.appendChild(wrap);
        });
    }

    function renderCourses(payload) {
        var courses = validateCourses(payload);
        clearCourses();
        if (!courses.length) {
            setCoursesState('empty');
            return;
        }

        var body = element('revenueByCourseBody');
        var cards = element('revenueByCourseCards');
        var snapshot = element('courseSnapshot');
        if (!body || !cards || !snapshot) {
            throw new Error('Revenue by course elements are missing.');
        }
        snapshot.innerHTML = '';
        document.querySelector('[data-course-count]').textContent = courses.length + (courses.length === 1 ? ' course' : ' courses');

        courses.forEach(function(course, index) {
            var row = document.createElement('tr');
            row.innerHTML = '<td><div class="teacher-table-title"></div></td><td></td><td></td><td></td><td></td><td></td>';
            row.children[0].querySelector('div').textContent = course.courseTitle || 'Untitled course';
            row.children[1].textContent = asMoney(course.revenue);
            row.children[2].textContent = asNumber(course.paidOrderCount);
            row.children[3].textContent = asNumber(course.unitsSold);
            row.children[4].textContent = asNumber(course.enrollmentCount);
            row.children[5].textContent = asNumber(course.studentCount);
            body.appendChild(row);

            var card = document.createElement('article');
            card.className = 'teacher-student-card';
            card.innerHTML = '<div class="teacher-table-title"></div><dl><div><dt>Revenue</dt><dd></dd></div><div><dt>Paid orders</dt><dd></dd></div><div><dt>Units sold</dt><dd></dd></div><div><dt>Enrollments</dt><dd></dd></div></dl>';
            card.querySelector('.teacher-table-title').textContent = course.courseTitle || 'Untitled course';
            var values = card.querySelectorAll('dd');
            values[0].textContent = asMoney(course.revenue);
            values[1].textContent = asNumber(course.paidOrderCount);
            values[2].textContent = asNumber(course.unitsSold);
            values[3].textContent = asNumber(course.enrollmentCount);
            cards.appendChild(card);

            if (index < 5) {
                var item = document.createElement('div');
                item.className = 'analytics-course-item';
                item.innerHTML = '<div class="analytics-course-line"><strong></strong><span></span></div><div class="analytics-course-meta"></div>';
                item.querySelector('strong').textContent = course.courseTitle || 'Untitled course';
                item.querySelector('span').textContent = asMoney(course.revenue);
                item.querySelector('.analytics-course-meta').textContent =
                    asNumber(course.paidOrderCount) + ' paid orders, ' + asNumber(course.enrollmentCount) + ' enrollments';
                snapshot.appendChild(item);
            }
        });
        setCoursesState('success');
    }

    function loadAnalytics() {
        if (state.loading) {
            return;
        }
        state.loading = true;
        setFormBusy(true);
        setPageLoading();
        clearSummary();
        clearCourses();
        setSummaryState('loading');
        setCoursesState('loading');

        var requests = {
            summary: fetchJson('/api/teacher/analytics/revenue?' + buildQuery(true), 'Revenue summary'),
            courses: fetchJson('/api/teacher/analytics/revenue/by-course?' + buildQuery(false), 'Revenue by course')
        };

        Promise.allSettled([requests.summary, requests.courses])
            .then(function(results) {
                setPageReady();
                if (results[0].status === 'fulfilled') {
                    try {
                        renderSummary(results[0].value);
                    } catch (err) {
                        clearSummary();
                        setSummaryState('error', 'Summary render failed: ' + err.message);
                    }
                } else {
                    clearSummary();
                    setSummaryState('error', results[0].reason.message);
                }

                if (results[1].status === 'fulfilled') {
                    try {
                        renderCourses(results[1].value);
                    } catch (err) {
                        clearCourses();
                        setCoursesState('error', 'Course render failed: ' + err.message);
                    }
                } else {
                    clearCourses();
                    setCoursesState('error', results[1].reason.message);
                }
            })
            .catch(function(err) {
                setPageError(err.message || 'Unable to load analytics.');
            })
            .finally(function() {
                state.loading = false;
                setFormBusy(false);
            });
    }

    form.addEventListener('submit', function(event) {
        event.preventDefault();
        loadAnalytics();
    });

    var resetButton = element('resetAnalyticsFilters');
    if (resetButton) {
        resetButton.addEventListener('click', function() {
            setDefaultDates();
            loadAnalytics();
        });
    }

    setDefaultDates();
    loadAnalytics();
});
