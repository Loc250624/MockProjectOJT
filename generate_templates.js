const fs = require('fs');
const path = require('path');

const baseDir = 'd:/Study/FPT-KY6/MockProject/Code/src/main/resources';

const createDir = (dir) => {
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }
};

const writeTemplate = (role, filename, title, specificContent) => {
    const filePath = path.join(baseDir, 'templates', role, filename);
    if (!fs.existsSync(filePath)) {
        const content = `<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title} - ${role.charAt(0).toUpperCase() + role.slice(1)} - OJTSU26 E-Learning</title>
    <link rel="stylesheet" th:href="@{/css/${role}/${role}.css}">
</head>
<body>
    <header th:replace="~{fragments/layout :: header('${role}')}"></header>
    <div class="layout-container">
        <aside th:replace="~{fragments/layout :: sidebar-${role}}"></aside>
        <main class="main-content">
            <h1>${title}</h1>
            ${specificContent}
        </main>
    </div>
    <footer th:replace="~{fragments/layout :: footer}"></footer>
    <script th:src="@{/js/${role}/${role}.js}"></script>
</body>
</html>`;
        fs.writeFileSync(filePath, content, 'utf8');
        console.log(`Created ${filePath}`);
    } else {
        console.log(`File ${filePath} already exists`);
    }
};

const writeStatic = (type, role, content) => {
    const ext = type === 'css' ? 'css' : 'js';
    const filePath = path.join(baseDir, 'static', type, role, `${role}.${ext}`);
    if (!fs.existsSync(filePath)) {
        fs.writeFileSync(filePath, content, 'utf8');
        console.log(`Created ${filePath}`);
    } else {
        console.log(`File ${filePath} already exists`);
    }
};

// 1. ADMIN
createDir(path.join(baseDir, 'templates', 'admin'));
createDir(path.join(baseDir, 'static', 'css', 'admin'));
createDir(path.join(baseDir, 'static', 'js', 'admin'));

const adminTemplates = [
    { file: 'dashboard.html', title: 'Dashboard', content: '<div class="stats-grid"><div class="stat-card"><h3>Total Users</h3><div class="value">150</div></div><div class="stat-card"><h3>Total Courses</h3><div class="value">24</div></div><div class="stat-card"><h3>Total Revenue</h3><div class="value">$1200</div></div><div class="stat-card"><h3>Active Students</h3><div class="value">80</div></div></div><table class="table"><thead><tr><th>Recent Activity</th></tr></thead><tbody><tr><td>User registered</td></tr></tbody></table>' },
    { file: 'users.html', title: 'Users Management', content: '<div class="search-bar"><input type="text" placeholder="Search users"><button>Search</button></div><a href="/admin/users/create" class="btn btn-primary">Add User</a><table class="table"><thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Admin User</td><td>admin@ojt.com</td><td>Admin</td><td>Active</td><td><button class="btn btn-info">View</button></td></tr></tbody></table>' },
    { file: 'user-detail.html', title: 'User Detail', content: '<div class="detail-card"><div class="detail-row"><span class="detail-label">Name</span><span class="detail-value">Admin User</span></div></div>' },
    { file: 'user-form.html', title: 'User Form', content: '<form class="form-container"><div class="form-group"><label>Name</label><input type="text"></div><div class="form-group"><label>Email</label><input type="email"></div><div class="form-group"><label>Role</label><select><option>Admin</option><option>Teacher</option><option>Student</option></select></div><button class="btn btn-primary">Submit</button></form>' },
    { file: 'courses.html', title: 'Courses Management', content: '<div class="search-bar"><input type="text" placeholder="Search courses"><button>Search</button></div><table class="table"><thead><tr><th>ID</th><th>Title</th><th>Teacher</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Java Spring Boot</td><td>John Doe</td><td>Active</td><td>2026-01-01</td><td><button class="btn btn-info">View</button></td></tr></tbody></table>' },
    { file: 'course-detail.html', title: 'Course Detail', content: '<div class="detail-card"><div class="detail-row"><span class="detail-label">Title</span><span class="detail-value">Java Spring Boot</span></div></div>' },
    { file: 'course-approval.html', title: 'Course Approval', content: '<table class="table"><thead><tr><th>ID</th><th>Title</th><th>Teacher</th><th>Submitted Date</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>New Course</td><td>Jane Doe</td><td>2026-06-01</td><td><button class="btn btn-success">Approve</button></td></tr></tbody></table>' },
    { file: 'categories.html', title: 'Categories Management', content: '<a href="/admin/categories/create" class="btn btn-primary">Add Category</a><table class="table"><thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Courses Count</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Programming</td><td>Code courses</td><td>5</td><td><button class="btn btn-info">Edit</button></td></tr></tbody></table>' },
    { file: 'category-form.html', title: 'Category Form', content: '<form class="form-container"><div class="form-group"><label>Name</label><input type="text"></div><div class="form-group"><label>Description</label><textarea></textarea></div><button class="btn btn-primary">Submit</button></form>' },
    { file: 'blogs.html', title: 'Blogs Management', content: '<table class="table"><thead><tr><th>ID</th><th>Title</th><th>Author</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Learning Java</td><td>Student A</td><td>Active</td><td>2026-06-10</td><td><button class="btn btn-info">View</button></td></tr></tbody></table>' },
    { file: 'blog-detail.html', title: 'Blog Detail', content: '<div class="detail-card"><div class="detail-row"><span class="detail-label">Title</span><span class="detail-value">Learning Java</span></div><div class="detail-row"><span class="detail-label">Content</span><span class="detail-value">Lorem ipsum...</span></div></div>' },
    { file: 'blog-moderation.html', title: 'Blog Moderation', content: '<table class="table"><thead><tr><th>ID</th><th>Title</th><th>Author</th><th>Submitted Date</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>New Blog</td><td>Student B</td><td>2026-06-20</td><td><button class="btn btn-success">Approve</button></td></tr></tbody></table>' },
    { file: 'comments.html', title: 'Comments Management', content: '<table class="table"><thead><tr><th>ID</th><th>Content Preview</th><th>Author</th><th>Blog/Course</th><th>Created</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Great course!</td><td>Student A</td><td>Java Course</td><td>2026-06-21</td><td><button class="btn btn-danger">Delete</button></td></tr></tbody></table>' },
    { file: 'payments.html', title: 'Payments', content: '<table class="table"><thead><tr><th>ID</th><th>User</th><th>Amount</th><th>Method</th><th>Status</th><th>Date</th></tr></thead><tbody><tr><td>1</td><td>Student A</td><td>$50</td><td>VNPAY</td><td>Success</td><td>2026-06-25</td></tr></tbody></table>' },
    { file: 'transactions.html', title: 'Transactions', content: '<table class="table"><thead><tr><th>ID</th><th>User</th><th>Course</th><th>Amount</th><th>Type</th><th>Status</th><th>Date</th></tr></thead><tbody><tr><td>1</td><td>Student A</td><td>Java Course</td><td>$50</td><td>Payment</td><td>Success</td><td>2026-06-25</td></tr></tbody></table>' },
    { file: 'refunds.html', title: 'Refunds', content: '<button class="btn btn-primary">Create Refund</button><table class="table"><thead><tr><th>ID</th><th>User</th><th>Amount</th><th>Reason</th><th>Status</th><th>Date</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Student B</td><td>$20</td><td>Duplicate purchase</td><td>Pending</td><td>2026-06-28</td><td><button class="btn btn-info">Process</button></td></tr></tbody></table>' },
    { file: 'analytics.html', title: 'Analytics', content: '<div class="stats-grid"><div class="stat-card"><h3>Total Users</h3><div class="value">150</div></div><div class="stat-card"><h3>Total Courses</h3><div class="value">24</div></div><div class="stat-card"><h3>Total Revenue</h3><div class="value">$1200</div></div><div class="stat-card"><h3>Active Students</h3><div class="value">80</div></div></div><div class="chart-placeholder">User Growth Chart</div><div class="chart-placeholder">Revenue Chart</div>' },
    { file: 'student-statistics.html', title: 'Student Statistics', content: '<div class="stats-grid"><div class="stat-card"><h3>Total Students</h3><div class="value">100</div></div><div class="stat-card"><h3>Active Students</h3><div class="value">80</div></div><div class="stat-card"><h3>New This Month</h3><div class="value">15</div></div></div><div class="chart-placeholder">Student Growth</div>' },
    { file: 'revenue-report.html', title: 'Revenue Report', content: '<div class="stats-grid"><div class="stat-card"><h3>Total Revenue</h3><div class="value">$1200</div></div><div class="stat-card"><h3>This Month</h3><div class="value">$400</div></div><div class="stat-card"><h3>This Year</h3><div class="value">$1200</div></div></div><div class="chart-placeholder">Revenue Over Time</div>' },
    { file: 'settings.html', title: 'Settings', content: '<form class="form-container"><div class="form-group"><label>Site Name</label><input type="text" value="OJTSU26 E-Learning"></div><div class="form-group"><label>Contact Email</label><input type="email" value="contact@ojt.com"></div><button class="btn btn-primary">Save</button></form>' },
    { file: 'system-settings.html', title: 'System Settings', content: '<form class="form-container"><div class="form-group"><label>Max Upload Size (MB)</label><input type="number" value="10"></div><div class="form-group"><label>Default Language</label><select><option>English</option><option>Vietnamese</option></select></div><button class="btn btn-primary">Save</button></form>' },
];
adminTemplates.forEach(t => writeTemplate('admin', t.file, t.title, t.content));
const adminCSS = `*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: system-ui, -apple-system, sans-serif; background: #f0f2f5; color: #333; }
.main-header { background: #1a1a2e; color: white; padding: 0.8rem 2rem; }
.header-container { display: flex; width: 100%; justify-content: space-between; align-items: center; }
.logo { color: white; text-decoration: none; font-size: 1.3rem; font-weight: bold; }
.header-nav a { color: #aaa; text-decoration: none; margin-left: 1.5rem; }
.header-nav a:hover { color: white; }
.layout-container { display: grid; grid-template-columns: 240px 1fr; min-height: calc(100vh - 120px); }
.sidebar { background: #16213e; padding: 1.5rem 0; }
.sidebar-nav { display: flex; flex-direction: column; }
.sidebar-nav h3 { color: #e94560; padding: 0 1.5rem; margin-bottom: 1rem; font-size: 0.9rem; text-transform: uppercase; letter-spacing: 1px; }
.sidebar-nav a { color: #ccc; text-decoration: none; padding: 0.6rem 1.5rem; transition: background 0.2s; }
.sidebar-nav a:hover { background: rgba(233,69,96,0.15); color: #e94560; }
.main-content { padding: 2rem; }
.main-content h1 { margin-bottom: 1.5rem; color: #1a1a2e; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1.2rem; margin-bottom: 2rem; }
.stat-card { background: white; border-radius: 8px; padding: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.stat-card h3 { color: #888; font-size: 0.85rem; text-transform: uppercase; margin-bottom: 0.5rem; }
.stat-card .value { font-size: 1.8rem; font-weight: bold; color: #1a1a2e; }
table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
th { background: #1a1a2e; color: white; padding: 0.8rem 1rem; text-align: left; font-size: 0.85rem; text-transform: uppercase; }
td { padding: 0.8rem 1rem; border-bottom: 1px solid #eee; }
tr:hover { background: #f8f9fa; }
.btn { display: inline-block; padding: 0.4rem 0.8rem; border: none; border-radius: 4px; cursor: pointer; font-size: 0.85rem; text-decoration: none; margin-right: 0.3rem; }
.btn-primary { background: #e94560; color: white; }
.btn-success { background: #27ae60; color: white; }
.btn-warning { background: #f39c12; color: white; }
.btn-danger { background: #e74c3c; color: white; }
.btn-info { background: #3498db; color: white; }
.btn:hover { opacity: 0.85; }
.search-bar { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; }
.search-bar input { flex: 1; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; font-size: 1rem; }
.search-bar button { background: #e94560; color: white; border: none; padding: 0.6rem 1.5rem; border-radius: 4px; cursor: pointer; }
.form-container { background: white; border-radius: 8px; padding: 2rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); max-width: 600px; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.3rem; font-weight: 600; color: #555; }
.form-group input, .form-group select, .form-group textarea { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; font-size: 1rem; box-sizing: border-box; }
.form-group textarea { min-height: 100px; resize: vertical; }
.detail-card { background: white; border-radius: 8px; padding: 2rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); margin-bottom: 1.5rem; }
.detail-row { display: flex; padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0; }
.detail-label { font-weight: 600; color: #555; width: 150px; }
.detail-value { color: #333; }
.chart-placeholder { background: white; border-radius: 8px; padding: 2rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); margin-bottom: 1.5rem; min-height: 300px; display: flex; align-items: center; justify-content: center; color: #aaa; font-size: 1.1rem; }
.action-bar { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; }
.main-footer { text-align: center; padding: 1rem; color: #666; background: #f0f2f5; border-top: 1px solid #ddd; }`;
writeStatic('css', 'admin', adminCSS);
writeStatic('js', 'admin', `'use strict';
console.log('Admin JS loaded');
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a');
    sidebarLinks.forEach(function(link) {
        if (link.getAttribute('href') === currentPath) {
            link.style.background = 'rgba(233,69,96,0.2)';
            link.style.color = '#e94560';
        }
    });
    var forms = document.querySelectorAll('form');
    forms.forEach(function(form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            alert('Form submission placeholder');
        });
    });
});`);

// 2. TEACHER
createDir(path.join(baseDir, 'templates', 'teacher'));
createDir(path.join(baseDir, 'static', 'css', 'teacher'));
createDir(path.join(baseDir, 'static', 'js', 'teacher'));

const teacherTemplates = [
    { file: 'dashboard.html', title: 'Teacher Dashboard', content: '<div class="stats-grid"><div class="stat-card"><h3>My Courses</h3><div class="value">3</div></div><div class="stat-card"><h3>Total Students</h3><div class="value">45</div></div><div class="stat-card"><h3>Total Revenue</h3><div class="value">$500</div></div><div class="stat-card"><h3>Average Rating</h3><div class="value">4.8</div></div></div><table class="table"><thead><tr><th>Recent Activity</th></tr></thead><tbody><tr><td>Student joined course</td></tr></tbody></table>' },
    { file: 'courses.html', title: 'My Courses', content: '<a href="/teacher/courses/create" class="btn btn-primary">Create Course</a><table class="table"><thead><tr><th>ID</th><th>Title</th><th>Students</th><th>Status</th><th>Rating</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Java Basics</td><td>20</td><td>Active</td><td>4.5</td><td><button class="btn btn-info">Edit</button></td></tr></tbody></table>' },
    { file: 'course-form.html', title: 'Course Form', content: '<form class="form-container"><div class="form-group"><label>Title</label><input type="text"></div><div class="form-group"><label>Description</label><textarea></textarea></div><div class="form-group"><label>Price</label><input type="number"></div><button class="btn btn-primary">Submit</button></form>' },
    { file: 'roadmap.html', title: 'Course Roadmap', content: '<div class="form-container"><div class="form-group"><label>Select Course</label><select><option>Java Basics</option></select></div><button class="btn btn-primary">Add Section</button></div><ul style="margin-top:20px;"><li>Section 1 <button class="btn btn-info">Edit</button></li></ul>' },
    { file: 'lessons.html', title: 'Lessons', content: '<div class="form-container"><div class="form-group"><label>Select Course</label><select><option>Java Basics</option></select></div><button class="btn btn-primary">Add Lesson</button></div><table class="table"><thead><tr><th>ID</th><th>Title</th><th>Section</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Intro</td><td>Section 1</td><td><button class="btn btn-info">Edit</button></td></tr></tbody></table>' },
    { file: 'lesson-editor.html', title: 'Lesson Editor', content: '<form class="form-container"><div class="form-group"><label>Title</label><input type="text"></div><div class="form-group"><label>Content</label><textarea></textarea></div><div class="form-group"><label>Video URL</label><input type="text"></div><button class="btn btn-primary">Save</button></form>' },
    { file: 'videos.html', title: 'Video Management', content: '<button class="btn btn-primary">Upload Video</button><table class="table"><thead><tr><th>ID</th><th>Title</th><th>Lesson</th><th>Duration</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Intro.mp4</td><td>Intro</td><td>5:00</td><td><button class="btn btn-danger">Delete</button></td></tr></tbody></table>' },
    { file: 'students.html', title: 'My Students', content: '<div class="form-container"><div class="form-group"><label>Select Course</label><select><option>Java Basics</option></select></div></div><table class="table"><thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Enrolled Date</th><th>Progress</th></tr></thead><tbody><tr><td>1</td><td>Student A</td><td>a@a.com</td><td>2026-06-01</td><td>50%</td></tr></tbody></table>' },
    { file: 'quizzes.html', title: 'Quizzes', content: '<a href="/teacher/quizzes/create" class="btn btn-primary">Create Quiz</a><table class="table"><thead><tr><th>ID</th><th>Title</th><th>Course</th><th>Questions</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Java Quiz 1</td><td>Java Basics</td><td>10</td><td><button class="btn btn-info">Edit</button></td></tr></tbody></table>' },
    { file: 'quiz-form.html', title: 'Quiz Form', content: '<form class="form-container"><div class="form-group"><label>Title</label><input type="text"></div><div class="form-group"><label>Course</label><select><option>Java Basics</option></select></div><div class="form-group"><label>Duration (mins)</label><input type="number"></div><button class="btn btn-primary">Save</button></form>' },
    { file: 'blogs.html', title: 'My Blogs', content: '<a href="/teacher/blogs/editor" class="btn btn-primary">Write New Blog</a><table class="table"><thead><tr><th>ID</th><th>Title</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Java Tips</td><td>Published</td><td>2026-06-15</td><td><button class="btn btn-info">Edit</button></td></tr></tbody></table>' },
    { file: 'blog-editor.html', title: 'Blog Editor', content: '<form class="form-container"><div class="form-group"><label>Title</label><input type="text"></div><div class="form-group"><label>Category</label><select><option>Programming</option></select></div><div class="form-group"><label>Content</label><textarea></textarea></div><button class="btn btn-primary">Submit for Review</button></form>' },
    { file: 'blog-submissions.html', title: 'Blog Submissions', content: '<table class="table"><thead><tr><th>ID</th><th>Title</th><th>Status</th><th>Reviewer Comments</th><th>Actions</th></tr></thead><tbody><tr><td>1</td><td>Java Tips</td><td>Rejected</td><td>Needs more info</td><td><button class="btn btn-info">Edit/Resubmit</button></td></tr></tbody></table>' },
    { file: 'analytics.html', title: 'Teacher Analytics', content: '<div class="stats-grid"><div class="stat-card"><h3>Total Revenue</h3><div class="value">$500</div></div></div><div class="chart-placeholder">Revenue Trend</div>' },
    { file: 'progress-report.html', title: 'Student Progress Report', content: '<div class="form-container"><div class="form-group"><label>Select Course</label><select><option>Java Basics</option></select></div></div><table class="table"><thead><tr><th>Student</th><th>Progress %</th><th>Quiz Score</th><th>Status</th></tr></thead><tbody><tr><td>Student A</td><td>75%</td><td>90</td><td>In Progress</td></tr></tbody></table>' }
];
teacherTemplates.forEach(t => writeTemplate('teacher', t.file, t.title, t.content));
const teacherCSS = adminCSS.replace(/#1a1a2e/g, '#1a2e1a').replace(/#16213e/g, '#1e3a1e').replace(/#e94560/g, '#27ae60');
writeStatic('css', 'teacher', teacherCSS);
writeStatic('js', 'teacher', `'use strict';
console.log('Teacher JS loaded');
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a');
    sidebarLinks.forEach(function(link) {
        if (link.getAttribute('href') === currentPath) {
            link.style.background = 'rgba(39,174,96,0.2)';
            link.style.color = '#27ae60';
        }
    });
});`);

// 3. STUDENT (Missing files)
const studentTemplates = [
    { file: 'results.html', title: 'My Results', content: '<table class="table"><thead><tr><th>Course</th><th>Assessment</th><th>Type</th><th>Score</th><th>Status</th></tr></thead><tbody><tr><td>Java Basics</td><td>Quiz 1</td><td>Quiz</td><td>90</td><td>Passed</td></tr></tbody></table>' },
    { file: 'certificates.html', title: 'My Certificates', content: '<div class="course-grid"><div class="course-card"><h3>Java Basics</h3><p>Completed: 2026-06-01</p><button class="btn btn-info">Download</button></div></div>' },
    { file: 'notifications.html', title: 'Notifications', content: '<button class="btn btn-primary mb-2">Mark All Read</button><ul style="list-style:none;padding:0;"><li style="background:#fff;padding:15px;margin-bottom:10px;border-left:3px solid #3498db;box-shadow:0 1px 3px rgba(0,0,0,0.1);">New course update posted!</li></ul>' },
    { file: 'checkout.html', title: 'Checkout', content: '<div class="checkout-container"><div class="order-summary"><h3>Java Basics - $50</h3></div><div class="payment-methods"><h3>Payment Method</h3><input type="radio" name="pay"> MoMo <br><input type="radio" name="pay"> VNPAY <br><br><button class="btn btn-primary">Place Order</button></div></div>' },
    { file: 'payment-result.html', title: 'Payment Result', content: '<div class="result-card"><h2>Payment Successful!</h2><p>Transaction ID: 123456</p><a href="/student/my-courses" class="btn btn-primary">Back to My Courses</a></div>' },
    { file: 'payment-history.html', title: 'Payment History', content: '<table class="table"><thead><tr><th>Transaction ID</th><th>Course</th><th>Amount</th><th>Method</th><th>Date</th></tr></thead><tbody><tr><td>123456</td><td>Java Basics</td><td>$50</td><td>MoMo</td><td>2026-06-25</td></tr></tbody></table>' },
    { file: 'blogs.html', title: 'Blogs', content: '<div class="search-bar"><input type="text"><button>Search</button></div><div class="course-grid"><div class="course-card"><h3>Learning Java</h3><p>By Student A</p><a href="/student/blogs/detail">Read More</a></div></div>' },
    { file: 'blog-detail.html', title: 'Blog Detail', content: '<div class="detail-card"><h1>Learning Java</h1><p>Content goes here...</p></div>' },
];
studentTemplates.forEach(t => writeTemplate('student', t.file, t.title, t.content));
const studentCSS = adminCSS.replace(/#1a1a2e/g, '#1a1a3e').replace(/#16213e/g, '#1e2a4e').replace(/#e94560/g, '#3498db') + `\n.course-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.2rem; } .course-card { background: white; border-radius: 8px; padding: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }`;
writeStatic('css', 'student', studentCSS);
writeStatic('js', 'student', `'use strict';
console.log('Student JS loaded');
document.addEventListener('DOMContentLoaded', function() {
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.sidebar-nav a');
    sidebarLinks.forEach(function(link) {
        if (link.getAttribute('href') === currentPath) {
            link.style.background = 'rgba(52,152,219,0.2)';
            link.style.color = '#3498db';
        }
    });
});`);
console.log('All missing files generated successfully!');
