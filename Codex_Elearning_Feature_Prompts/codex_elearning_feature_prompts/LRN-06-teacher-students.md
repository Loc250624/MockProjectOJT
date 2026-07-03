# LRN-06 — Teacher quản lý danh sách học viên trong khóa học

- **Mã chức năng:** LRN-06
- **Module:** Teacher Portal
- **Actor chính:** Teacher
- **Mức ưu tiên:** Cao
- **Phụ thuộc:** Course ownership, Enrollment, Authentication/RBAC

## Mục tiêu

Cho phép Teacher xem và quản lý danh sách Student đã đăng ký trong các khóa học do chính Teacher phụ trách, có tìm kiếm, lọc, phân trang và thao tác phù hợp với policy.

## Quy tắc nghiệp vụ

1. Teacher chỉ truy cập danh sách học viên của course mình sở hữu/phụ trách.
2. Admin có thể có quyền rộng hơn theo policy hiện tại; không mở quyền này cho Teacher.
3. Không cho phép Teacher xem học viên course khác bằng đổi course ID.
4. Danh sách chỉ trả dữ liệu cần thiết: tên, avatar, email nếu policy cho phép, enrollment status, enrolledAt, progress summary.
5. Không trả password hash, OAuth identifiers, token, địa chỉ hoặc dữ liệu nhạy cảm.
6. Search/filter/sort phải thực hiện server-side với pagination.
7. Trạng thái enrollment phải dùng enum hợp lệ.
8. Thao tác quản lý có thể gồm xem chi tiết, lọc trạng thái, export nếu hệ thống đã yêu cầu; không tự thêm chức năng xóa user.
9. Nếu cho phép Teacher thay đổi enrollment status, phải có audit, transition hợp lệ và không được sửa payment history.
10. Không để một Teacher trực tiếp chỉnh sửa điểm/progress từ màn danh sách này nếu chưa có use case riêng.

## Phạm vi triển khai

### Backend
- Query danh sách theo course ownership ở cấp repository/service.
- API phân trang với search theo tên/email và filter enrollment status/progress.
- Trả DTO tối thiểu, có total elements/pages.
- Kiểm tra sort field allowlist để tránh lỗi/injection.
- Tối ưu query aggregate progress, tránh N+1.
- Nếu có status action:
  - Xác thực transition.
  - Ghi audit.
  - Tạo notification khi phù hợp.

### Frontend
- Teacher course detail → tab `Students`.
- Bảng/card responsive gồm student, enrolled date, status, progress và action hợp lệ.
- Search có debounce; filter, sort và pagination giữ state trên URL nếu convention dự án hỗ trợ.
- Empty state, no-result, loading, error.
- Mobile không giữ sidebar/table vượt màn hình; dùng responsive cards hoặc horizontal scroll hợp lý.
- Không hiển thị action không có quyền.

### Export
- Chỉ triển khai nếu repository đã có pattern hoặc yêu cầu dự án.
- Export phải áp dụng cùng ownership/filter và giới hạn dữ liệu.

## Test bắt buộc

- Teacher owner xem được học viên course của mình.
- Teacher khác bị 403/404 theo convention dự án.
- Đổi course ID không lộ danh sách.
- Search/filter/sort/pagination chính xác.
- Response không chứa field nhạy cảm.
- Query không phát sinh N+1 theo từng Student.
- Admin behavior đúng policy.
- Status transition trái phép bị từ chối và không sửa dữ liệu.
- UI responsive và giữ filter khi chuyển trang.

## Tiêu chí nghiệm thu

- Teacher quản lý được danh sách học viên cho từng course của mình.
- Ownership được kiểm tra ở backend cho mọi endpoint.
- Dữ liệu trả về tối thiểu và có phân trang.
- Không lộ dữ liệu Student ngoài phạm vi.
- Hiệu năng không giảm tuyến tính do N+1 ở mỗi dòng.

## Nguyên tắc bắt buộc dành cho Codex

1. Đọc `AGENTS.md`, `README.md`, tài liệu kiến trúc, cấu hình build và quy ước trong repository trước khi thay đổi mã nguồn.
2. Khảo sát toàn bộ luồng liên quan từ entity/model → repository → service → controller/API → security → giao diện → test.
3. Tận dụng cấu trúc, naming convention, DTO, mapper, exception handler, response format và design system đang tồn tại; không tạo kiến trúc song song.
4. Không sửa hoặc format hàng loạt các file ngoài phạm vi chức năng.
5. Không xóa hành vi hiện có chỉ để làm test mới chạy.
6. Không hard-code user, role, course, price, trạng thái, URL, secret hoặc dữ liệu môi trường.
7. Mọi quyền truy cập phải được kiểm tra ở backend; việc ẩn nút trên frontend không được xem là biện pháp bảo mật.
8. Với thay đổi database, ưu tiên migration tương thích ngược. Không drop/rename cột đang dùng nếu chưa chứng minh an toàn.
9. Bổ sung unit test và integration test cho luồng thành công, từ chối truy cập và trường hợp biên.
10. Chạy các lệnh build/test phù hợp với repository. Nếu lệnh nào không chạy được, ghi rõ nguyên nhân và phần đã kiểm tra thay thế.
11. Cuối tác vụ, báo cáo:
    - Những file đã thay đổi.
    - Migration hoặc cấu hình mới.
    - Test đã chạy và kết quả.
    - Giả định còn tồn tại.
    - Cách kiểm thử thủ công.

---

## Prompt hoàn chỉnh cho Codex

> Sao chép nguyên khối nội dung bên dưới vào Codex khi đang mở đúng repository và đúng feature branch.

```text
Hãy triển khai **LRN-06 — Teacher quản lý danh sách học viên trong khóa học**.

### Kết quả cần đạt
- Trong Teacher Portal, mỗi course của Teacher có tab/page Students.
- Có danh sách phân trang, tìm kiếm, lọc và sắp xếp.
- Hiển thị dữ liệu tối thiểu phù hợp: Student, enrollment status/date và progress summary.
- Teacher chỉ xem được course do mình sở hữu/phụ trách.
- Không lộ field nhạy cảm và không phát sinh N+1.

### Hãy thực hiện
- Khảo sát mapping Teacher–Course và Enrollment hiện có.
- Đặt kiểm tra ownership trong service/backend, không chỉ ở UI.
- Tạo/tái sử dụng DTO, pageable response và query projection/aggregate phù hợp.
- Allowlist sort fields.
- Chỉ thêm thao tác đổi enrollment status nếu nghiệp vụ/codebase đã có; khi có, validate transition và audit.
- Không thêm chức năng xóa Student, sửa payment hoặc chỉnh progress ngoài use case.
- Hoàn thiện UI responsive, loading, empty, no-result, error và pagination.
- Viết test ownership/IDOR, pagination, search/filter/sort, sensitive fields, N+1 và status transition.

### Cách làm việc bắt buộc

- Trước tiên, hãy đọc `AGENTS.md` và khảo sát repository. Không bắt đầu viết code ngay.
- Hãy xác định các thành phần đã tồn tại và lập một kế hoạch ngắn theo đúng kiến trúc hiện tại.
- Chỉ triển khai phạm vi của chức năng này và những thay đổi phụ thuộc trực tiếp.
- Không tạo entity, endpoint, service hoặc trang trùng với thành phần hiện có.
- Không sửa các chức năng không liên quan.
- Bảo đảm authorization ở backend, validation đầu vào, xử lý lỗi nhất quán và không lộ dữ liệu nhạy cảm.
- Thêm hoặc cập nhật test phù hợp.
- Chạy build/test của dự án.
- Sau khi hoàn tất, tự review diff để tìm lỗi logic, N+1 query, IDOR, race condition, lỗi trạng thái và regression.
- Trả về báo cáo gồm: kế hoạch đã thực hiện, file thay đổi, test đã chạy, kết quả, cách kiểm thử thủ công và các giả định.
```
