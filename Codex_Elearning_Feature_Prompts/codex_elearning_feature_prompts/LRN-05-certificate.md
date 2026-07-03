# LRN-05 — Cấp và tải chứng chỉ khi hoàn thành đủ điều kiện

- **Mã chức năng:** LRN-05
- **Module:** Learning Experience / Certificate
- **Actor chính:** Student
- **Mức ưu tiên:** Trung bình–Cao
- **Phụ thuộc:** LRN-03 Progress, ASM-01/Assessment, Course completion rules

## Mục tiêu

Tự động cấp chứng chỉ cho Student khi enrollment đáp ứng đầy đủ điều kiện hoàn thành; cho phép xem và tải chứng chỉ có mã xác thực duy nhất.

## Quy tắc nghiệp vụ

1. Chứng chỉ chỉ được cấp khi backend tự xác minh đủ điều kiện.
2. Điều kiện lấy từ course/config hiện có, ví dụ:
   - Hoàn thành toàn bộ lesson bắt buộc.
   - Đạt quiz/assignment bắt buộc.
   - Enrollment hợp lệ và course cho phép cấp chứng chỉ.
3. Client không được gửi `eligible=true`, điểm, phần trăm hoặc tên tùy ý để yêu cầu cấp.
4. Mỗi enrollment/course completion chỉ có tối đa một chứng chỉ hiệu lực.
5. Việc cấp phải idempotent và an toàn khi hai request chạy đồng thời.
6. Certificate snapshot phải giữ thông tin tại thời điểm cấp: tên Student, tên Course, Teacher/issuer, ngày cấp, mã xác thực.
7. Không dùng ID tăng dần làm public verification code.
8. Student chỉ tải chứng chỉ của mình; Admin/Teacher có quyền xem theo policy hiện tại.
9. File PDF phải được tạo từ dữ liệu server-side đã sanitize.
10. Nếu certificate bị revoke, verification phải phản ánh trạng thái đó.

## Phạm vi triển khai

### Backend
- Rà soát entity `Certificate`, completion service và assessment result.
- Tạo service `evaluateEligibility`/tương đương làm nguồn sự thật dùng chung.
- Tự động gọi cấp chứng chỉ khi completion thay đổi hoặc cung cấp endpoint idempotent để claim; chọn theo kiến trúc hiện tại.
- Tạo verification code ngẫu nhiên/UUID đủ khó đoán.
- API:
  - Lấy chứng chỉ của Student hiện tại.
  - Tải PDF.
  - Xác minh công khai bằng code với dữ liệu tối thiểu.
- PDF generation phải tái sử dụng template/branding hiện có; xử lý font Unicode.
- Không lưu file tạm nhạy cảm lâu hơn cần thiết.
- Tránh path traversal trong tên file.

### Frontend
- Trạng thái chưa đủ điều kiện, đủ điều kiện/đang cấp, đã cấp.
- Nút View/Download certificate.
- Trang xác minh hiển thị hợp lệ, revoked hoặc không tồn tại.
- Không hiển thị dữ liệu cá nhân ngoài mức cần thiết.

### Audit
- Lưu issuedAt, issuedBy/system source, revokedAt/reason nếu model hỗ trợ.

## Test bắt buộc

- Chưa đủ lesson/assessment thì không cấp.
- Đủ điều kiện thì cấp đúng một chứng chỉ.
- Request đồng thời không tạo hai chứng chỉ.
- Student khác không tải được certificate.
- Verification code không đoán được từ database ID.
- PDF chứa đúng snapshot và tải với content type/filename hợp lệ.
- Tên có Unicode được render đúng.
- Revoked certificate không được báo hợp lệ.
- Curriculum hoặc tên course thay đổi sau cấp không làm sai snapshot đã phát hành.

## Tiêu chí nghiệm thu

- Certificate chỉ xuất hiện sau khi backend xác minh completion.
- Download PDF hoạt động và không lộ file hệ thống.
- Verification công khai dùng code an toàn.
- Không có duplicate certificate.
- Thay đổi dữ liệu course/user sau cấp không làm thay đổi certificate snapshot ngoài ý muốn.

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
Hãy triển khai **LRN-05 — Cấp, xem, tải và xác minh chứng chỉ**.

### Kết quả cần đạt
- Backend tự tính eligibility từ progress và assessment thật.
- Khi Student đủ điều kiện, hệ thống cấp duy nhất một certificate cho enrollment/course completion.
- Student có thể xem và tải PDF.
- Có verification code công khai khó đoán và trang/API xác minh tối thiểu.
- Hỗ trợ trạng thái revoked nếu model hiện tại có hoặc có yêu cầu tương ứng.

### Hãy thực hiện
- Khảo sát Certificate, Enrollment, Progress, Quiz/Assignment result và completion policy hiện có.
- Tạo một service eligibility dùng chung; không lặp logic giữa controller và job/event.
- Không tin bất kỳ cờ completion, điểm hoặc tên nào từ client.
- Dùng transaction + unique constraint để bảo đảm idempotency/concurrency.
- Lưu snapshot thông tin tại lúc cấp.
- Tạo PDF server-side an toàn, hỗ trợ Unicode và không path traversal.
- Bảo vệ endpoint download theo ownership.
- Viết test eligibility, duplicate/concurrent issuance, IDOR, PDF, Unicode, verification code và revoked state.

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
