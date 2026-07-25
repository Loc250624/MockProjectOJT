# File Discovery Guide

Codex nên dùng `rg`/IDE search với các nhóm từ khóa sau, sau đó xác minh bằng import tree:

```text
LumiNa, Logo, Brand, Navbar, Header, Topbar, Sidebar, MobileMenu, Drawer
TeacherLayout, TeacherPortal, AdminLayout, AdminPortal, StudentLayout
CoursePlayer, HelpGuide, Roadmap, Courses, AllCourses, MyCourses
DataTable, Table, SearchInput, Breadcrumb, Footer, DashboardHero
```

Tìm anti-pattern CSS:

```text
min-width:
width: 1000px / 1200px / 1440px
white-space: nowrap
position: absolute
left: / right:
margin-left (liên quan sidebar)
overflow-x: hidden
transform: scale
font-size rất nhỏ trong media query
```

Không sửa chỉ vì khớp keyword. Phải trace component tới route bị lỗi và xác nhận nguyên nhân.
