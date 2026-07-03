# ASM-01 — Student làm bài trắc nghiệm, lưu tạm và nộp bài

- **Mã chức năng:** ASM-01
- **Module:** Assessment
- **Actor chính:** Student
- **Mức ưu tiên:** Cao
- **Phụ thuộc:** Authentication/RBAC, Enrollment, Quiz/Question/Option, Curriculum

## Mục tiêu

Cho phép Student đủ quyền bắt đầu bài trắc nghiệm, trả lời câu hỏi, tự động lưu tạm, tiếp tục attempt chưa nộp và nộp bài để hệ thống chấm điểm theo cấu hình.

## Quy tắc nghiệp vụ

1. Chỉ Student enrolled và quiz đang khả dụng mới được bắt đầu.
2. Backend quyết định quiz open/close time, attempt limit, duration và passing score.
3. Mỗi attempt có trạng thái rõ: in-progress, submitted, expired/auto-submitted hoặc trạng thái tương đương.
4. Không gửi đáp án đúng, điểm từng option hoặc explanation bí mật trước khi submit.
5. Question order/option order phải được cố định trong attempt nếu quiz randomize.
6. Autosave phải idempotent và chỉ cập nhật attempt của Student hiện tại.
7. Không cho sửa answer sau khi attempt đã submitted/expired.
8. Đồng hồ phía client chỉ để hiển thị; thời hạn thật được tính ở backend.
9. Submit phải transaction-safe và chỉ chấm một lần.
10. Request submit lặp trả kết quả cũ, không tạo attempt/result mới.
11. Nếu hết giờ, backend từ chối save tiếp và auto-submit/expire theo policy.
12. Chấm điểm:
    - Không tin điểm từ client.
    - Hỗ trợ single-choice/multiple-choice theo model hiện có.
    - Áp dụng rounding và pass rule nhất quán.
13. Chỉ hiển thị answer/explanation sau submit nếu quiz setting cho phép.
14. Không cho Student truy cập attempt/result của người khác bằng đổi ID.

## Phạm vi triển khai

### Data model
- Rà soát Quiz, Question, Option, Attempt/QuizResult và Answer.
- Bổ sung attempt entity nếu chưa có, nhưng không nhân đôi `QuizResult` hiện hữu.
- Cần lưu snapshot hoặc reference đủ ổn định để quiz thay đổi giữa attempt không làm sai chấm điểm.
- Unique/constraint phù hợp cho answer theo attempt + question.

### Backend
- Start/resume attempt.
- Lấy question payload an toàn, không có correct-answer metadata.
- Autosave một hoặc nhiều answer.
- Submit/auto-submit và chấm điểm server-side.
- Lấy result của Student hiện tại.
- Kiểm tra enrollment, availability, attempt limit, deadline và ownership trong service.
- Dùng transaction và locking/versioning cho save/submit cạnh tranh.
- Không log đáp án hoặc thông tin nhạy cảm không cần thiết.

### Frontend
- Quiz introduction: số câu, thời lượng, attempt còn lại và quy tắc.
- Question navigator, trạng thái answered/unanswered, Previous/Next.
- Autosave có trạng thái Saving/Saved/Error và retry an toàn.
- Timer dựa trên `expiresAt` từ server; đồng bộ lại khi refresh.
- Cảnh báo câu chưa trả lời trước submit.
- Resume attempt sau refresh/đăng nhập lại.
- Result page theo quiz setting.
- Responsive và keyboard accessible.

### Chống gian lận cơ bản
- Không coi các biện pháp client-side là bảo mật.
- Không lộ đáp án qua JSON, HTML, source map hoặc hidden field.
- Rate-limit hợp lý cho autosave/start/submit nếu dự án có hạ tầng.

## Test bắt buộc

- Student enrolled bắt đầu và resume attempt.
- Student chưa enrolled/quiz đóng bị từ chối.
- Payload question không chứa đáp án đúng.
- Autosave tạo/cập nhật answer, request lặp không duplicate.
- Student không sửa attempt của người khác.
- Hết giờ không save được và được xử lý theo policy.
- Submit chấm đúng single-choice và multiple-choice.
- Submit lặp không chấm/tạo result lần hai.
- Attempt limit được áp dụng.
- Hai request save/submit đồng thời không làm hỏng dữ liệu.
- Randomized order giữ ổn định trong cùng attempt.
- Refresh UI giữ answer và timer đúng.

## Tiêu chí nghiệm thu

- Student có thể bắt đầu, lưu tạm, resume và submit quiz.
- Backend là nguồn sự thật cho thời gian, đáp án và điểm.
- Không lộ đáp án trước khi được phép.
- Không thể thao tác attempt của user khác.
- Submit có tính idempotent và kết quả ổn định.

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
Hãy triển khai **ASM-01 — Student làm bài trắc nghiệm, tự động lưu tạm, tiếp tục và nộp bài**.

### Kết quả cần đạt
- Student enrolled có thể start/resume một quiz khả dụng.
- Câu hỏi và option được trả về mà không lộ đáp án đúng.
- Answer được autosave idempotent.
- Timer dùng `expiresAt` do backend tính; refresh không reset thời gian.
- Submit được chấm hoàn toàn ở backend, áp dụng attempt limit, passing score và quiz setting.
- Submit lặp hoặc request đồng thời không tạo result/chấm điểm hai lần.
- Result page chỉ hiển thị đáp án/explanation theo cấu hình quiz.

### Hãy thực hiện
- Khảo sát Quiz/Question/Option/QuizResult/Attempt hiện có và tránh tạo model trùng.
- Kiểm tra enrollment, ownership, availability, open/close time, attempt limit và duration trong service.
- Cố định question/option order cho attempt nếu randomize.
- Dùng transaction, optimistic/pessimistic locking hoặc version phù hợp cho autosave và submit.
- Không tin timer, score, correct answer hoặc submitted state từ client.
- Không để correct-answer metadata xuất hiện trong DTO/JSON/HTML trước thời điểm cho phép.
- Hoàn thiện UI responsive, autosave status, question navigator, unanswered warning, resume và result.
- Viết test security/IDOR, answer leakage, expiry, attempt limit, scoring, duplicate submit, concurrent save-submit và randomized order.

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
