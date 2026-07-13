'use strict';

document.addEventListener('DOMContentLoaded', function() {
    var moneyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

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

    function number(value) {
        return Number(value || 0).toLocaleString('en-US');
    }

    function money(value) {
        return moneyFormatter.format(Number(value || 0));
    }

    function renderSingleBarChart(container, points, valueKey, labelBuilder) {
        container.innerHTML = '';
        if (!points.length) {
            container.innerHTML = '<div class="empty-state"><div class="empty-state-title">No data found.</div></div>';
            return;
        }
        var maxValue = points.reduce(function(max, point) {
            return Math.max(max, Number(point[valueKey] || 0));
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
            var label = document.createElement('div');
            label.className = 'admin-chart-label';
            label.textContent = point.period;
            barWrap.appendChild(bar);
            group.appendChild(barWrap);
            group.appendChild(label);
            container.appendChild(group);
        });
    }

    function renderStudentBarChart(container, points) {
        container.innerHTML = '';
        if (!points.length) {
            container.innerHTML = '<div class="empty-state"><div class="empty-state-title">No data found.</div></div>';
            return;
        }
        var maxValue = points.reduce(function(max, point) {
            return Math.max(max, Number(point.newStudents || 0), Number(point.activeStudents || 0));
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
            var activeBar = document.createElement('div');
            activeBar.className = 'admin-chart-bar secondary';
            activeBar.style.height = barHeight(point.activeStudents, maxValue);
            activeBar.title = point.period + ': ' + number(point.activeStudents) + ' active students';
            var label = document.createElement('div');
            label.className = 'admin-chart-label';
            label.textContent = point.period;
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
        return Math.max(3, Math.round((Number(value || 0) / maxValue) * 210)) + 'px';
    }

    function initDashboard() {
        var form = document.getElementById('adminDashboardFilters');
        if (!form) return;

        function load() {
            setState('adminDashboard', 'loading');
            var params = new URLSearchParams();
            appendDates(params, form);
            Promise.all([
                fetchApi('/api/admin/dashboard/overview?' + params.toString()),
                fetchApi('/api/admin/analytics/students?' + params.toString() + '&groupBy=day'),
                fetchApi('/api/admin/analytics/revenue?' + params.toString() + '&groupBy=day')
            ])
                .then(function(results) {
                    renderOverview(results[0]);
                    renderStudentBarChart(document.getElementById('dashboardStudentsChart'), results[1].trend || []);
                    renderSingleBarChart(document.getElementById('dashboardRevenueChart'), results[2].trend || [], 'revenue', function(point) {
                        return point.period + ': ' + money(point.revenue) + ', ' + number(point.paidOrderCount) + ' paid orders';
                    });
                    setState('adminDashboard', 'content');
                })
                .catch(function(error) {
                    setState('adminDashboard', 'error', error.message);
                });
        }

        function renderOverview(data) {
            document.querySelectorAll('[data-overview]').forEach(function(node) {
                node.textContent = number(data[node.getAttribute('data-overview')]);
            });
            document.querySelectorAll('[data-overview-money]').forEach(function(node) {
                node.textContent = money(data[node.getAttribute('data-overview-money')]);
            });
            document.querySelector('[data-overview-period]').textContent = data.from + ' to ' + data.to;
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

        function load() {
            setState('adminStudents', 'loading');
            var params = new URLSearchParams();
            appendDates(params, form);
            fetchApi('/api/admin/analytics/students?' + params.toString())
                .then(function(data) {
                    document.querySelector('[data-students-new]').textContent = number(data.newStudents);
                    document.querySelector('[data-students-active]').textContent = number(data.activeStudents);
                    document.querySelector('[data-students-method]').textContent = data.activeStudentMethod;
                    document.querySelector('[data-students-period]').textContent = data.from + ' to ' + data.to;
                    document.getElementById('adminStudentsEmpty').hidden = Number(data.newStudents || 0) > 0 || Number(data.activeStudents || 0) > 0;
                    renderStudentBarChart(document.getElementById('adminStudentsChart'), data.trend || []);
                    renderStudentsTable(data.trend || []);
                    setState('adminStudents', 'content');
                })
                .catch(function(error) {
                    setState('adminStudents', 'error', error.message);
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

    function renderStudentsTable(points) {
        document.getElementById('adminStudentsTableBody').innerHTML = points.map(function(point) {
            return '<tr><td>' + escape(point.period) + '</td><td>' + number(point.newStudents) + '</td><td>' + number(point.activeStudents) + '</td></tr>';
        }).join('');
    }

    function initRevenue() {
        var form = document.getElementById('adminRevenueAnalyticsFilters');
        if (!form) return;

        function load() {
            setState('adminRevenue', 'loading');
            var params = new URLSearchParams();
            appendDates(params, form);
            fetchApi('/api/admin/analytics/revenue?' + params.toString())
                .then(function(data) {
                    document.querySelector('[data-revenue-total]').textContent = money(data.totalRevenue);
                    document.querySelector('[data-revenue-orders]').textContent = number(data.paidOrderCount);
                    document.querySelector('[data-revenue-period]').textContent = data.from + ' to ' + data.to;
                    document.getElementById('adminRevenueEmpty').hidden = Number(data.totalRevenue || 0) > 0 || Number(data.paidOrderCount || 0) > 0;
                    renderSingleBarChart(document.getElementById('adminRevenueChart'), data.trend || [], 'revenue', function(point) {
                        return point.period + ': ' + money(point.revenue) + ', ' + number(point.paidOrderCount) + ' paid orders';
                    });
                    renderRevenueTable(data.trend || []);
                    setState('adminRevenue', 'content');
                })
                .catch(function(error) {
                    setState('adminRevenue', 'error', error.message);
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

    function renderRevenueTable(points) {
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
