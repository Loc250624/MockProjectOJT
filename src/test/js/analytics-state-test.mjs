import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';

class ElementStub {
  constructor(id = '') {
    this.id = id;
    this.hidden = false;
    this.textContent = '';
    this.children = [];
    this.attributes = {};
    this.style = {};
    this.className = '';
    this.elements = {};
    this._innerHTML = '';
  }

  set innerHTML(value) {
    this._innerHTML = String(value);
    this.children = [];
    if (this._innerHTML.includes('<td')) {
      for (let index = 0; index < 6; index += 1) {
        const cell = new ElementStub();
        cell.querySelector = () => new ElementStub();
        this.children.push(cell);
      }
    }
  }

  get innerHTML() {
    return this._innerHTML;
  }

  setAttribute(name, value) {
    this.attributes[name] = value;
  }

  getAttribute(name) {
    return this.attributes[name] ?? null;
  }

  appendChild(child) {
    this.children.push(child);
    return child;
  }

  addEventListener(type, callback) {
    this[`on${type}`] = callback;
  }

  querySelector() {
    return new ElementStub();
  }

  querySelectorAll(selector) {
    if (selector === 'dd') {
      return [new ElementStub(), new ElementStub(), new ElementStub(), new ElementStub()];
    }
    return [];
  }
}

function createDocument() {
  const ids = new Map();
  const attrs = new Map();
  const callbacks = {};

  function id(name) {
    const node = new ElementStub(name);
    ids.set(name, node);
    return node;
  }

  function attr(selector, attributes = {}) {
    const node = new ElementStub(selector);
    Object.entries(attributes).forEach(([key, value]) => node.setAttribute(key, value));
    attrs.set(selector, node);
    return node;
  }

  return {
    ids,
    attrs,
    callbacks,
    addEventListener(type, callback) {
      callbacks[type] = callback;
    },
    createElement() {
      return new ElementStub();
    },
    getElementById(name) {
      return ids.get(name) ?? null;
    },
    querySelector(selector) {
      return attrs.get(selector) ?? null;
    },
    querySelectorAll(selector) {
      if (selector === '[data-kpi-period]') {
        return [...attrs.entries()]
          .filter(([key]) => key.startsWith('[data-kpi-period'))
          .map(([, node]) => node);
      }
      if (selector === '[data-overview]') {
        return [...attrs.entries()]
          .filter(([key]) => key.startsWith('[data-overview='))
          .map(([, node]) => node);
      }
      if (selector === '[data-overview-money]') {
        return [...attrs.entries()]
          .filter(([key]) => key.startsWith('[data-overview-money='))
          .map(([, node]) => node);
      }
      if (selector === '[data-overview], [data-overview-money]') {
        return [...attrs.entries()]
          .filter(([key]) => key.startsWith('[data-overview=') || key.startsWith('[data-overview-money='))
          .map(([, node]) => node);
      }
      return [];
    }
  };
}

function addForm(document, idName, elementNames) {
  const form = document.ids.get(idName) ?? new ElementStub(idName);
  form.elements = {};
  elementNames.forEach((name) => {
    form.elements[name] = new ElementStub(name);
  });
  document.ids.set(idName, form);
  return form;
}

function addTeacherDom(document) {
  addForm(document, 'teacherAnalyticsFilters', ['from', 'to', 'groupBy', 'courseId']);
  [
    'analyticsLoading', 'analyticsError', 'analyticsContent',
    'analyticsSummaryLoading', 'analyticsSummaryError', 'analyticsSummaryEmpty', 'analyticsSummaryContent',
    'analyticsCoursesLoading', 'analyticsCoursesError', 'analyticsCoursesEmpty', 'analyticsCoursesContent',
    'revenueTrendChart', 'revenueByCourseBody', 'revenueByCourseCards', 'courseSnapshot', 'resetAnalyticsFilters'
  ].forEach((name) => document.ids.set(name, new ElementStub(name)));
  ['analyticsError', 'analyticsSummaryError', 'analyticsCoursesError'].forEach((name) => {
    document.ids.get(name).querySelector = () => new ElementStub(`${name}-message`);
  });
  [
    '[data-total-revenue]', '[data-paid-orders]', '[data-paid-students]', '[data-enrollments]',
    '[data-students]', '[data-best-course]', '[data-period-label]', '[data-course-count]'
  ].forEach((selector) => document.attrs.set(selector, new ElementStub(selector)));
  document.attrs.set('[data-kpi-period-revenue]', Object.assign(new ElementStub(), {
    getAttribute: (name) => (name === 'data-kpi-unit' ? 'USD' : null)
  }));
  document.attrs.set('[data-kpi-period-orders]', Object.assign(new ElementStub(), {
    getAttribute: (name) => (name === 'data-kpi-unit' ? 'orders' : null)
  }));
}

function addDashboardDom(document) {
  addForm(document, 'adminDashboardFilters', ['from', 'to']);
  [
    'adminDashboardLoading', 'adminDashboardError', 'adminDashboardContent',
    'dashboardOverviewLoading', 'dashboardOverviewError', 'dashboardOverviewEmpty', 'dashboardOverviewContent',
    'dashboardStudentsChart', 'dashboardRevenueChart'
  ].forEach((name) => document.ids.set(name, new ElementStub(name)));
  ['adminDashboardError', 'dashboardOverviewError'].forEach((name) => {
    document.ids.get(name).querySelector = () => new ElementStub(`${name}-message`);
  });
  document.attrs.set('[data-reset-dashboard]', new ElementStub('[data-reset-dashboard]'));
  document.attrs.set('[data-overview-period]', new ElementStub('[data-overview-period]'));
  document.attrs.set('[data-overview-active-method]', new ElementStub('[data-overview-active-method]'));
  ['totalUsers', 'totalStudents', 'totalTeachers', 'totalCourses', 'totalEnrollments', 'paidOrderCount', 'newStudents', 'activeStudents']
    .forEach((field) => {
      document.attrs.set(`[data-overview=${field}]`, Object.assign(new ElementStub(), {
        getAttribute: () => field
      }));
    });
  document.attrs.set('[data-overview-money=totalRevenue]', Object.assign(new ElementStub(), {
    getAttribute: () => 'totalRevenue'
  }));
}

function addStudentsDom(document) {
  addForm(document, 'adminStudentsAnalyticsFilters', ['from', 'to', 'groupBy']);
  [
    'adminStudentsLoading', 'adminStudentsError', 'adminStudentsContent',
    'adminStudentsEmpty', 'adminStudentsChart', 'adminStudentsTableBody'
  ].forEach((name) => document.ids.set(name, new ElementStub(name)));
  document.ids.get('adminStudentsError').querySelector = () => new ElementStub('students-message');
  ['[data-reset-students]', '[data-students-new]', '[data-students-active]', '[data-students-method]', '[data-students-period]', '[data-students-chart-period]', '[data-students-timezone]']
    .forEach((selector) => document.attrs.set(selector, new ElementStub(selector)));
}

function addRevenueDom(document) {
  addForm(document, 'adminRevenueAnalyticsFilters', ['from', 'to', 'groupBy']);
  [
    'adminRevenueLoading', 'adminRevenueError', 'adminRevenueContent',
    'adminRevenueEmpty', 'adminRevenueChart', 'adminRevenueTableBody'
  ].forEach((name) => document.ids.set(name, new ElementStub(name)));
  document.ids.get('adminRevenueError').querySelector = () => new ElementStub('revenue-message');
  ['[data-reset-revenue]', '[data-revenue-total]', '[data-revenue-orders]', '[data-revenue-period]', '[data-revenue-chart-period]', '[data-revenue-timezone]', '[data-revenue-method]']
    .forEach((selector) => document.attrs.set(selector, new ElementStub(selector)));
}

async function runScript(file, document, responses) {
  const urls = [];
  const context = {
    document,
    window: { location: { origin: 'http://127.0.0.1:8080', pathname: '/' } },
    URLSearchParams,
    Intl,
    Number,
    String,
    Math,
    Array,
    fetch: async (url) => {
      urls.push(url);
      const next = responses.shift();
      if (next instanceof Error) {
        throw next;
      }
      return {
        ok: next.ok !== false,
        json: async () => next.body
      };
    }
  };
  vm.createContext(context);
  vm.runInContext(fs.readFileSync(file, 'utf8'), context);
  document.callbacks.DOMContentLoaded();
  await new Promise((resolve) => setTimeout(resolve, 0));
  await new Promise((resolve) => setTimeout(resolve, 0));
  return urls;
}

const teacherSummary = {
  code: 200,
  data: {
    from: '2026-06-15',
    to: '2026-07-14',
    currency: 'USD',
    totalRevenue: 10,
    paidOrderCount: 1,
    paidStudentCount: 1,
    enrollmentCount: 1,
    studentCount: 1,
    bestSellingCourse: null,
    trend: [{ period: '2026-07-14', revenue: 10, paidOrderCount: 1 }]
  }
};

const teacherCourses = {
  code: 200,
  data: {
    courses: [{ courseTitle: 'Course A', revenue: 10, paidOrderCount: 1, unitsSold: 1, enrollmentCount: 1, studentCount: 1 }]
  }
};

const overview = {
  code: 200,
  data: {
    from: '2026-06-15',
    to: '2026-07-14',
    totalUsers: 1,
    totalStudents: 1,
    totalTeachers: 0,
    totalCourses: 1,
    totalEnrollments: 1,
    paidOrderCount: 1,
    totalRevenue: 10,
    newStudents: 1,
    activeStudents: 1,
    activeStudentMethod: 'method'
  }
};

const students = {
  code: 200,
  data: {
    from: '2026-06-15',
    to: '2026-07-14',
    timeZone: 'Asia/Bangkok',
    newStudents: 1,
    activeStudents: 1,
    activeStudentMethod: 'method',
    trend: [{ period: '2026-07-14', newStudents: 1, activeStudents: 1 }]
  }
};

const revenue = {
  code: 200,
  data: {
    from: '2026-06-15',
    to: '2026-07-14',
    timeZone: 'Asia/Bangkok',
    groupBy: 'day',
    totalRevenue: 10,
    paidOrderCount: 1,
    revenueRecognitionMethod: 'method',
    trend: [{ period: '2026-07-14', revenue: 10, paidOrderCount: 1 }]
  }
};

{
  const document = createDocument();
  addTeacherDom(document);
  const urls = await runScript('src/main/resources/static/js/teacher/analytics.js', document, [
    { body: teacherSummary },
    { ok: false, body: { code: 500, message: 'Course request failed' } }
  ]);
  assert.equal(urls.length, 2, 'teacher init should call two APIs once');
  assert.equal(document.ids.get('analyticsLoading').hidden, true);
  assert.equal(document.ids.get('analyticsError').hidden, true);
  assert.equal(document.ids.get('analyticsContent').hidden, false);
  assert.equal(document.ids.get('analyticsSummaryContent').hidden, false);
  assert.equal(document.ids.get('analyticsCoursesError').hidden, false);
  assert.equal(document.ids.get('analyticsCoursesContent').hidden, true);
}

{
  const document = createDocument();
  addTeacherDom(document);
  const urls = await runScript('src/main/resources/static/js/teacher/analytics.js', document, [
    { ok: false, body: { code: 500, message: 'Summary request failed' } },
    { body: teacherCourses }
  ]);
  assert.equal(urls.length, 2, 'teacher partial failure should not retry or duplicate APIs');
  assert.equal(document.ids.get('analyticsLoading').hidden, true);
  assert.equal(document.ids.get('analyticsSummaryError').hidden, false);
  assert.equal(document.ids.get('analyticsCoursesContent').hidden, false);
}

{
  const document = createDocument();
  addDashboardDom(document);
  const urls = await runScript('src/main/resources/static/js/admin/analytics.js', document, [
    { body: overview },
    { body: students },
    { ok: false, body: { code: 500, message: 'Revenue request failed' } }
  ]);
  assert.equal(urls.length, 3, 'dashboard init should call three APIs once');
  assert.equal(document.ids.get('adminDashboardLoading').hidden, true);
  assert.equal(document.ids.get('adminDashboardError').hidden, true);
  assert.equal(document.ids.get('adminDashboardContent').hidden, false);
  assert.equal(document.ids.get('dashboardOverviewContent').hidden, false);
  assert.match(document.ids.get('dashboardRevenueChart').innerHTML, /admin-chart-state-error/);
}

{
  const document = createDocument();
  addStudentsDom(document);
  const urls = await runScript('src/main/resources/static/js/admin/analytics.js', document, [{ body: students }]);
  assert.equal(urls.length, 1, 'student analytics init should call one API');
  assert.equal(document.ids.get('adminStudentsLoading').hidden, true);
  assert.equal(document.ids.get('adminStudentsError').hidden, true);
  assert.equal(document.ids.get('adminStudentsContent').hidden, false);
}

{
  const document = createDocument();
  addRevenueDom(document);
  const urls = await runScript('src/main/resources/static/js/admin/analytics.js', document, [{ body: revenue }]);
  assert.equal(urls.length, 1, 'revenue analytics init should call one API');
  assert.equal(document.ids.get('adminRevenueLoading').hidden, true);
  assert.equal(document.ids.get('adminRevenueError').hidden, true);
  assert.equal(document.ids.get('adminRevenueContent').hidden, false);
}

console.log('analytics-state-test passed');
