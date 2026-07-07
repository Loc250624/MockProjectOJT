'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var form = document.getElementById('teacherAnalyticsFilters');
    if (!form) {
        return;
    }

    var loading = document.getElementById('analyticsLoading');
    var content = document.getElementById('analyticsContent');
    var error = document.getElementById('analyticsError');
    var empty = document.getElementById('analyticsEmpty');
    var formatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

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

    function showLoading() {
        loading.hidden = false;
        content.hidden = true;
        error.hidden = true;
    }

    function showError(message) {
        loading.hidden = true;
        content.hidden = true;
        error.hidden = false;
        error.querySelector('[data-error-message]').textContent = message || 'Please try again.';
    }

    function showContent() {
        loading.hidden = true;
        content.hidden = false;
        error.hidden = true;
    }

    function asMoney(value) {
        return formatter.format(Number(value || 0));
    }

    function asNumber(value) {
        return Number(value || 0).toLocaleString('en-US');
    }

    function renderSummary(summary) {
        document.querySelector('[data-total-revenue]').textContent = asMoney(summary.totalRevenue);
        document.querySelector('[data-paid-orders]').textContent = asNumber(summary.paidOrderCount);
        document.querySelector('[data-enrollments]').textContent = asNumber(summary.enrollmentCount);
        document.querySelector('[data-students]').textContent = asNumber(summary.studentCount);
        document.querySelector('[data-best-course]').textContent = summary.bestSellingCourse
            ? summary.bestSellingCourse.courseTitle
            : 'None yet';
        document.querySelector('[data-period-label]').textContent = summary.from + ' to ' + summary.to;
        empty.hidden = Number(summary.totalRevenue || 0) > 0 || Number(summary.paidOrderCount || 0) > 0;
        renderTrend(summary.trend || []);
    }

    function renderTrend(points) {
        var chart = document.getElementById('revenueTrendChart');
        chart.innerHTML = '';
        if (!points.length) {
            chart.innerHTML = '<div class="teacher-state report-inline-state"><strong>No trend data.</strong><span>No paid orders were found for this period.</span></div>';
            return;
        }
        var maxRevenue = points.reduce(function(max, point) {
            return Math.max(max, Number(point.revenue || 0));
        }, 0);
        points.forEach(function(point) {
            var wrap = document.createElement('div');
            wrap.className = 'analytics-bar-wrap';
            var bar = document.createElement('div');
            bar.className = 'analytics-bar';
            var height = maxRevenue === 0 ? 3 : Math.max(3, Math.round((Number(point.revenue || 0) / maxRevenue) * 220));
            bar.style.height = height + 'px';
            bar.title = point.period + ': ' + asMoney(point.revenue) + ' from ' + asNumber(point.paidOrderCount) + ' paid orders';
            var label = document.createElement('div');
            label.className = 'analytics-bar-label';
            label.textContent = point.period;
            wrap.appendChild(bar);
            wrap.appendChild(label);
            chart.appendChild(wrap);
        });
    }

    function renderCourses(courses) {
        var body = document.getElementById('revenueByCourseBody');
        var cards = document.getElementById('revenueByCourseCards');
        var snapshot = document.getElementById('courseSnapshot');
        body.innerHTML = '';
        cards.innerHTML = '';
        snapshot.innerHTML = '';
        document.querySelector('[data-course-count]').textContent = courses.length + (courses.length === 1 ? ' course' : ' courses');

        if (!courses.length) {
            body.innerHTML = '<tr><td colspan="6">No courses found for this teacher.</td></tr>';
            snapshot.innerHTML = '<div class="teacher-state report-inline-state"><strong>No courses found.</strong><span>Create a course to start tracking revenue.</span></div>';
            return;
        }

        courses.forEach(function(course, index) {
            var row = document.createElement('tr');
            row.innerHTML = '<td><div class="teacher-table-title"></div></td><td></td><td></td><td></td><td></td><td></td>';
            row.children[0].querySelector('div').textContent = course.courseTitle;
            row.children[1].textContent = asMoney(course.revenue);
            row.children[2].textContent = asNumber(course.paidOrderCount);
            row.children[3].textContent = asNumber(course.unitsSold);
            row.children[4].textContent = asNumber(course.enrollmentCount);
            row.children[5].textContent = asNumber(course.studentCount);
            body.appendChild(row);

            var card = document.createElement('article');
            card.className = 'teacher-student-card';
            card.innerHTML = '<div class="teacher-table-title"></div><dl><div><dt>Revenue</dt><dd></dd></div><div><dt>Paid orders</dt><dd></dd></div><div><dt>Units sold</dt><dd></dd></div><div><dt>Enrollments</dt><dd></dd></div></dl>';
            card.querySelector('.teacher-table-title').textContent = course.courseTitle;
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
                item.querySelector('strong').textContent = course.courseTitle;
                item.querySelector('span').textContent = asMoney(course.revenue);
                item.querySelector('.analytics-course-meta').textContent =
                    asNumber(course.paidOrderCount) + ' paid orders, ' + asNumber(course.enrollmentCount) + ' enrollments';
                snapshot.appendChild(item);
            }
        });
    }

    function loadAnalytics() {
        showLoading();
        Promise.all([
            fetch('/api/teacher/analytics/revenue?' + buildQuery(true), { headers: { 'Accept': 'application/json' } }),
            fetch('/api/teacher/analytics/revenue/by-course?' + buildQuery(false), { headers: { 'Accept': 'application/json' } })
        ])
            .then(function(responses) {
                return Promise.all(responses.map(function(response) {
                    return response.json().then(function(payload) {
                        if (!response.ok || payload.code >= 400) {
                            throw new Error(payload.message || 'Unable to load analytics.');
                        }
                        return payload.data;
                    });
                }));
            })
            .then(function(results) {
                renderSummary(results[0]);
                renderCourses(results[1].courses || []);
                showContent();
            })
            .catch(function(err) {
                showError(err.message);
            });
    }

    form.addEventListener('submit', function(event) {
        event.preventDefault();
        loadAnalytics();
    });
    document.getElementById('resetAnalyticsFilters').addEventListener('click', function() {
        setDefaultDates();
        loadAnalytics();
    });

    setDefaultDates();
    loadAnalytics();
});
