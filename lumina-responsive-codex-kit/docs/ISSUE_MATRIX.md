# Responsive Issue Matrix

| Ref | Khu vực | Triệu chứng | Nguyên nhân có khả năng | Hướng sửa ưu tiên |
|---|---|---|---|---|
| 01 | Public mobile navigation | Logo/menu/search/button chiếm nhiều chiều cao, bố cục rời rạc | Header desktop bị thu nhỏ thay vì chuyển mode | Mobile drawer/menu riêng, spacing/token nhất quán |
| 02 | Student Help Guide | Card/tab/text sát biên, tab dễ ép chữ | Fixed padding/width, tab flex không wrap | Container fluid, min-width:0, tabs responsive |
| 03 | Course player header | Logo khác hệ thống, actions và avatar khó xếp | Brand component riêng, fixed widths/absolute | Shared Logo, responsive action grid/stack |
| 04 | Public header | Mục menu cuối bị cắt bên phải | Breakpoint quá muộn, flex child không shrink | Chuyển mobile sớm hơn, min-width:0, gap clamp |
| 05 | Public mobile menu | Navigation/menu trigger không cân đối | Hard-coded widths/margins | Grid header 3 vùng hoặc logo + trigger chuẩn |
| 06/09/11 | Teacher dashboard | Email dài và action bị cắt bên phải | Hero 2 cột cố định, nowrap/fixed button | One-column mobile, wrap, overflow-wrap |
| 07 | Teacher Courses | Table bị nén cực nhỏ trên iPhone SE | Table desktop bị scale/fit vào viewport | Mobile cards, desktop table, controls stack |
| 08 | Teacher Roadmap | Description/table vượt ngang, Add button lệch | Min-width table/fixed columns, header row nowrap | Mobile card list/scroll wrapper, header stack |
| 10 | Admin Courses | Table bị nén cực nhỏ, chữ khó đọc | Cùng anti-pattern với Teacher Courses | Shared responsive data-list/table component |

## Những lỗi thường đi kèm cần tìm

- `width: 1200px`, `min-width` lớn hoặc `white-space: nowrap` trên container chính.
- Flex/grid child thiếu `min-width: 0`.
- Button group không `wrap`.
- Header dùng absolute positioning để canh action.
- Table bị đặt `font-size` cực nhỏ trên mobile.
- Sidebar/topbar không có mode mobile riêng.
- Footer có row cố định gây tràn.
- Breadcrumb không truncate/wrap.
- Icon/logo có nhiều implementation khác nhau.
